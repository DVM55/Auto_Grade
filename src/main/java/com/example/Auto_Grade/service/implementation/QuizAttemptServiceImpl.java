package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.AnswerRequest;
import com.example.Auto_Grade.dto.req.SaveAnswerRequest;
import com.example.Auto_Grade.dto.res.*;
import com.example.Auto_Grade.entity.*;
import com.example.Auto_Grade.enums.*;
import com.example.Auto_Grade.integration.minio.MinioChannel;
import com.example.Auto_Grade.repository.*;
import com.example.Auto_Grade.service.QuizAttemptService;
import com.example.Auto_Grade.utils.AttemptHelper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;

@Service
@RequiredArgsConstructor
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final QuestionOptionRepository optionRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionConfigRepository configRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final QuestionRepository questionRepository;

    private final AttemptHelper attemptHelper;
    private final MinioChannel minioChannel;

    @Override
    @Transactional
    public QuizAttemptResultResponse submitQuiz(Long attemptId, List<SaveAnswerRequest> requests) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        QuizAttempt attempt = getValidAttempt(attemptId, accountId);

        List<Question> questions = getQuestionsForAttempt(attempt);

        for (SaveAnswerRequest req : requests) {

            Question question = questions.stream()
                    .filter(q -> q.getId().equals(req.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Câu hỏi không thuộc quiz"));

            QuizAnswer answer = answerRepository
                    .findByAttemptIdAndQuestionId(attemptId, req.getQuestionId())
                    .orElse(QuizAnswer.builder()
                            .attempt(attempt)
                            .question(question)
                            .build());

            switch (question.getQuestionType()) {

                case SHORT_ANSWER -> answer.setAnswerText(req.getAnswerText());

                case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                    List<QuestionOption> opts = optionRepository
                            .findAllById(req.getSelectedOptionIds());

                    answer.getSelectedOptions().clear();
                    answer.getSelectedOptions().addAll(opts);
                }
            }

            boolean correct = isCorrect(question, answer);
            answer.setIsCorrect(correct);

            answerRepository.save(answer);
        }

        // 🔥 submit luôn
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());

        attemptRepository.save(attempt);

        return calculateResult(attempt, questions);
    }

    @Override
    @Transactional
    public StartQuizResponse startQuiz(Long quizId) {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy quiz"));

        // --- Kiểm tra trạng thái quiz ---
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new IllegalArgumentException("Quiz chưa được xuất bản");
        }

        // --- Kiểm tra thời gian ---
        LocalDateTime now = LocalDateTime.now();
        if (quiz.getStartTime() != null && now.isBefore(quiz.getStartTime())) {
            throw new IllegalArgumentException("Quiz chưa đến giờ mở");
        }
        if (quiz.getEndTime() != null && now.isAfter(quiz.getEndTime())) {
            throw new IllegalArgumentException("Quiz đã hết hạn");
        }

        if (quiz.getAccessType() == QuizAccessType.PRIVATE) {
            boolean inClass = quiz.getClasses().stream()
                    .flatMap(c -> c.getMembers().stream())  // List<ClassMember>
                    .anyMatch(m -> m.getAccount().getId().equals(accountId)
                            && m.getStatus() == MemberStatus.APPROVED);  // phải là thành viên được duyệt
            if (!inClass) {
                throw new AccessDeniedException("Bạn không có quyền truy cập quiz này");
            }
        }

        Optional<QuizAttempt> existingAttempt = attemptRepository
                .findByQuizIdAndCreator_IdAndStatus(quizId, accountId, AttemptStatus.IN_PROGRESS);

        if (existingAttempt.isPresent()) {
            QuizAttempt ongoing = existingAttempt.get();

            if (ongoing.getExpiredAt() != null && now.isAfter(ongoing.getExpiredAt())) {
                attemptHelper.expireAttempt(ongoing);
            } else {
                return buildOngoingAttemptResponse(ongoing);
            }
        }


        if (quiz.getMaxAttempts() != null) {
            long done = attemptRepository.countByQuizIdAndCreator_Id(quizId, accountId);
            if (done >= quiz.getMaxAttempts()) {
                throw new IllegalArgumentException("Bạn đã hết lượt làm bài");
            }
        }

        System.out.println("→ Creating new attempt...");

        // --- Tạo attempt mới ---
        LocalDateTime expiredAt = quiz.getDurationMinutes() != null
                ? now.plusMinutes(quiz.getDurationMinutes())
                : null;

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .creator(Account.builder().id(accountId).build())
                .startedAt(now)
                .expiredAt(expiredAt)
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        attemptRepository.save(attempt);

        // ── Lấy câu hỏi theo mode ──
        List<Question> questions;

        if (Boolean.TRUE.equals(quiz.getIsRandom())) {
            List<QuizQuestionConfig> configs = configRepository.findByQuizId(quizId);
            questions = new ArrayList<>();

            Long creatorId = quiz.getCreator().getId(); // ← thêm

            for (QuizQuestionConfig config : configs) {
                List<Question> pool = questionRepository.findByFilters(
                        creatorId,                          // ← thêm
                        config.getCategoryQuestionId(),
                        config.getGroupQuestionId()
                );

                if (pool.size() < config.getQuantity()) {
                    throw new IllegalArgumentException("Không đủ câu hỏi trong ngân hàng để tạo bài thi.");
                }

                Collections.shuffle(pool);
                questions.addAll(pool.subList(0, config.getQuantity()));
            }

            Collections.shuffle(questions);

            List<AttemptQuestion> attemptQuestions = new ArrayList<>();
            for (Question question : questions) {
                attemptQuestions.add(AttemptQuestion.builder()
                        .attempt(attempt)
                        .question(question)
                        .build());
            }
            attemptQuestionRepository.saveAll(attemptQuestions);

        } else {
            questions = quiz.getQuestions();
        }

        // --- Build response (câu hỏi không có đáp án đúng) ---
        List<QuestionQuizAttemptResponse> questionQuizAttemptResponses = questions.stream()
                .map(q -> toQuestionQuizAttemptResponseWithAnswered(q, null))
                .collect(Collectors.toList());

        return new StartQuizResponse(
                attempt.getId(),
                quiz.getTitle(),
                quiz.getDurationMinutes(),
                attempt.getStartedAt(),
                expiredAt,
                questionQuizAttemptResponses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<QuizAttemptResultResponse> getMyAttemptHistory(Long quizId, int page, int size) {
        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());

        Page<QuizAttempt> attemptPage = attemptRepository
                .findByQuizIdAndCreator_IdAndStatus(quizId, accountId, AttemptStatus.SUBMITTED, pageable);

        List<QuizAttemptResultResponse> data = attemptPage.getContent().stream()
                .map(this::toResultResponse)
                .toList();

        MetaResponse meta = MetaResponse.builder()
                .totalItems(attemptPage.getTotalElements())
                .itemCount(attemptPage.getNumberOfElements())
                .itemsPerPage(attemptPage.getSize())
                .totalPages(attemptPage.getTotalPages())
                .currentPage(attemptPage.getNumber() + 1)
                .build();

        return PagingResponse.<QuizAttemptResultResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy lịch sử làm bài thành công")
                .data(data)
                .meta(meta)
                .build();
    }

    @Override
    public List<QuizAttemptAnswerResponse> getAttemptAnswers(Long attemptId) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy attempt"));

        boolean isAttemptOwner = attempt.getCreator().getId().equals(accountId);
        boolean isQuizOwner    = attempt.getQuiz().getCreator().getId().equals(accountId);

        if (!isAttemptOwner && !isQuizOwner) {
            throw new AccessDeniedException("Không có quyền xem");
        }

        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new IllegalArgumentException("Bài chưa được nộp");
        }

        List<QuizAnswer> answers = answerRepository.findByAttemptId(attemptId);

        Map<Long, QuizAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        Quiz quiz = attempt.getQuiz();
        List<Question> questions = getQuestionsForAttempt(attempt);
        return questions.stream()
                .map(q -> buildAnswerResponse(q, answerMap.get(q.getId()), quiz))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<QuizResultResponse> getAllResultsByQuiz(Long quizId, int page, int size, String userName, String email) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy quiz"));

        // 🔐 chỉ giáo viên tạo quiz mới xem được
        if (!quiz.getCreator().getId().equals(accountId)) {
            throw new AccessDeniedException("Bạn không có quyền xem danh sách bài nộp");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());

        String keyword = normalizeKeyword(userName);

        Page<QuizAttempt> attemptPage;

        if (hasAccent(keyword)) {
            // có dấu
            System.out.println("Searching with accent for keyword: " + keyword);
            attemptPage =attemptRepository
                    .searchByUsernameAndEmail(quizId, AttemptStatus.SUBMITTED, keyword, email, pageable);
        } else {
            // không dấu
            System.out.println("Searching searchWithoutAccent: " + keyword);
            attemptPage =attemptRepository
                    .searchWithoutAccent(quizId, AttemptStatus.SUBMITTED, keyword, email, pageable);
        }

        List<QuizResultResponse> data = attemptPage.getContent().stream()
                .map(this::toQuizResultResponse)
                .toList();

        MetaResponse meta = MetaResponse.builder()
                .totalItems(attemptPage.getTotalElements())
                .itemCount(attemptPage.getNumberOfElements())
                .itemsPerPage(attemptPage.getSize())
                .totalPages(attemptPage.getTotalPages())
                .currentPage(attemptPage.getNumber() + 1)
                .build();

        return PagingResponse.<QuizResultResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy tất cả bài đã nộp thành công")
                .data(data)
                .meta(meta)
                .build();
    }

    @Override
    @Transactional
    public void saveAnswer(AnswerRequest request) {
        QuizAttempt attempt = attemptRepository.findByIdWithLock(request.getAttemptId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy attempt"));

        if (!attempt.getCreator().getId().equals(request.getAccountId()))
            throw new AccessDeniedException("Bạn không có quyền thao tác bài này");

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS)
            throw new IllegalArgumentException("Bài đã được nộp hoặc đã hết hạn");

        if (attempt.getExpiredAt() != null && LocalDateTime.now().isAfter(attempt.getExpiredAt().plusMinutes(1))) {
            attemptHelper.expireAttempt(attempt);
            throw new IllegalArgumentException("Quá hạn thời gian nộp bài");
        }

        List<Question> questions = getQuestionsForAttempt(attempt); // ← thêm

        Question question = questions.stream() // ← đổi
                .filter(q -> q.getId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Câu hỏi không thuộc quiz"));

        QuizAnswer answer = answerRepository
                .findByAttemptIdAndQuestionId(request.getAttemptId(), request.getQuestionId())
                .orElse(QuizAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .build());

        switch (question.getQuestionType()) {

            case SHORT_ANSWER -> answer.setAnswerText(request.getAnswerText());

            case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                List<QuestionOption> opts = optionRepository
                        .findAllById(request.getSelectedOptionIds());

                answer.getSelectedOptions().clear();
                answer.getSelectedOptions().addAll(opts);
            }
        }

        boolean correct = isCorrect(question, answer);
        answer.setIsCorrect(correct);

        answerRepository.save(answer);
    }

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<QuizResult> getAllResults(int page, int size) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());

        Page<QuizAttempt> attemptPage = attemptRepository
                .findByCreator_IdAndStatus(accountId, AttemptStatus.SUBMITTED, pageable);

        List<QuizResult> data = attemptPage.getContent().stream()
                .map(attempt -> QuizResult.builder()
                        .id(attempt.getId())
                        .correctCount(attempt.getCorrectCount())
                        .totalQuestions(getQuestionsForAttempt(attempt).size())
                        .totalScore(attempt.getTotalScore())
                        .submittedAt(attempt.getSubmittedAt())
                        .allowReview(attempt.getQuiz().getAllowReview())
                        .quizTitle(attempt.getQuiz().getTitle())
                        .build())
                .toList();

        MetaResponse meta = MetaResponse.builder()
                .totalItems(attemptPage.getTotalElements())
                .itemCount(attemptPage.getNumberOfElements())
                .itemsPerPage(attemptPage.getSize())
                .totalPages(attemptPage.getTotalPages())
                .currentPage(attemptPage.getNumber() + 1)
                .build();

        return PagingResponse.<QuizResult>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy tất cả bài đã nộp thành công")
                .data(data)
                .meta(meta)
                .build();
    }


    private QuizAttemptResultResponse calculateResult(QuizAttempt attempt, List<Question> questions) {

        List<QuizAnswer> allAnswers = answerRepository.findByAttemptId(attempt.getId());
        int totalQuestions = questions.size();

        Quiz quiz = attempt.getQuiz();

        int correctCount = (int) allAnswers.stream()
                .filter(ans -> Boolean.TRUE.equals(ans.getIsCorrect()))
                .count();

        // --- Tính tổng điểm ---
        double totalScore;
        if (Boolean.TRUE.equals(quiz.getAutoScore())) {
            double scorePerQuestion = (quiz.getTotalScore() != null ? quiz.getTotalScore() : 0.0)
                    / totalQuestions;
            double rawScore = scorePerQuestion * correctCount;
            totalScore = round(rawScore * 100.0) / 100.0;
        } else {
            totalScore = allAnswers.stream()
                    .filter(ans -> isCorrect(ans.getQuestion(), ans))
                    .mapToDouble(ans -> ans.getQuestion().getScore() != null
                            ? ans.getQuestion().getScore() : 0.0)
                    .sum();
        }

        attempt.setCorrectCount(correctCount);
        attempt.setTotalScore(totalScore);
        attemptRepository.save(attempt);

        return new QuizAttemptResultResponse(
                attempt.getId(),
                correctCount,
                totalQuestions,
                totalScore,
                LocalDateTime.now(),
                quiz.getAllowReview()
        );
    }

    private boolean isCorrect(Question question, QuizAnswer answer) {
        return switch (question.getQuestionType()) {

            case SINGLE_CHOICE -> {
                if (answer.getSelectedOptions().isEmpty()) yield false;
                Long selectedId = answer.getSelectedOptions().getFirst().getId();
                yield question.getOptions().stream()
                        .anyMatch(o -> o.getId().equals(selectedId)
                                && Boolean.TRUE.equals(o.getIsCorrect()));
            }

            case MULTIPLE_CHOICE -> {
                Set<Long> correctIds = question.getOptions().stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                        .map(QuestionOption::getId)
                        .collect(Collectors.toSet());

                Set<Long> selectedIds = answer.getSelectedOptions().stream()
                        .map(QuestionOption::getId)
                        .collect(Collectors.toSet());

                yield correctIds.equals(selectedIds);
            }

            case SHORT_ANSWER -> {
                if (answer.getAnswerText() == null || answer.getAnswerText().isBlank())
                    yield false;
                String ans = answer.getAnswerText().trim().replaceAll("\\s+", " ");
                yield question.getShortAnswerOptions().stream()
                        .anyMatch(o -> o.getAnswerText().equals(ans));
            }
        };
    }

    // ----------------------------------------------------------------
    // Trả lại bài đang làm dở kèm đáp án đã chọn
    // ----------------------------------------------------------------
    private StartQuizResponse buildOngoingAttemptResponse(QuizAttempt attempt) {

        List<QuizAnswer> savedAnswers = answerRepository.findByAttemptId(attempt.getId());

        Map<Long, QuizAnswer> answerMap = savedAnswers.stream()
                .collect(Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        a -> a
                ));

        List<Question> questions = getQuestionsForAttempt(attempt);

        List<QuestionQuizAttemptResponse> questionQuizAttemptResponses = questions.stream()
                .map(q -> toQuestionQuizAttemptResponseWithAnswered(q, answerMap.get(q.getId())))
                .collect(Collectors.toList());

        return new StartQuizResponse(
                attempt.getId(),
                attempt.getQuiz().getTitle(),
                attempt.getQuiz().getDurationMinutes(),
                attempt.getStartedAt(),
                attempt.getExpiredAt(),
                questionQuizAttemptResponses
        );
    }

    private List<Question> getQuestionsForAttempt(QuizAttempt attempt) {
        if (Boolean.TRUE.equals(attempt.getQuiz().getIsRandom())) {
            return attemptQuestionRepository
                    .findByAttemptIdOrderByCreatedAt(attempt.getId())
                    .stream()
                    .map(AttemptQuestion::getQuestion)
                    .toList();
        }
        return attempt.getQuiz().getQuestions();
    }

    // ----------------------------------------------------------------
    // Build QuestionResponse kèm đáp án đã chọn (null nếu chưa chọn)
    // ----------------------------------------------------------------
    private QuestionQuizAttemptResponse toQuestionQuizAttemptResponseWithAnswered(Question q, QuizAnswer savedAnswer) {

        List<OptionResponse> options = null;
        if (q.getQuestionType() != QuestionType.SHORT_ANSWER) {
            options = q.getOptions().stream()
                    .map(o -> new OptionResponse(o.getId(), o.getOptionText()))
                    .collect(Collectors.toList());
        }

        List<Long> selectedOptionIds = null;
        String answerText = null;

        if (savedAnswer != null) {
            selectedOptionIds = savedAnswer.getSelectedOptions().stream()
                    .map(QuestionOption::getId)
                    .collect(Collectors.toList());
            answerText = savedAnswer.getAnswerText();
        }

        return new QuestionQuizAttemptResponse(
                q.getId(),
                q.getContent(),
                q.getQuestionType(),
                q.getMediaObjectKey() != null ? minioChannel.getPresignedUrlSafe(q.getMediaObjectKey(),3600) : null,
                q.getMediaType(),       // ← thêm mediaType
                options,
                selectedOptionIds,
                answerText
        );
    }

    private QuizAttempt getValidAttempt(Long attemptId, Long accountId) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy attempt"));

        if (!attempt.getCreator().getId().equals(accountId)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác bài này");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Bài đã được nộp hoặc đã hết hạn");
        }

        if (attempt.getExpiredAt() != null
                && LocalDateTime.now().isAfter(attempt.getExpiredAt().plusMinutes(1))) {
            attemptHelper.expireAttempt(attempt);
            throw new IllegalArgumentException("Quá hạn thời gian nộp bài (Hệ thống đã lưu kết quả thời gian thực trong lúc làm bài)");
        }

        return attempt;
    }

    private QuizAttemptResultResponse toResultResponse(QuizAttempt attempt) {
        int totalQuestions = getQuestionsForAttempt(attempt).size();

        return new QuizAttemptResultResponse(
                attempt.getId(),
                attempt.getCorrectCount(),
                totalQuestions,
                attempt.getTotalScore(),
                attempt.getSubmittedAt(),
                attempt.getQuiz().getAllowReview()
        );
    }

    private QuizAttemptAnswerResponse buildAnswerResponse(Question q, QuizAnswer answer, Quiz quiz) {

        List<QuestionOptionResponse> options = null;
        List<ShortAnswerOptionResponse> correctAnswers = null;
        List<Long> selectedOptionIds = null;
        String answeredText = null;

        // --- Options ---
        if (q.getQuestionType() != QuestionType.SHORT_ANSWER) {
            options = q.getOptions().stream()
                    .map(o -> new QuestionOptionResponse(
                            o.getId(),
                            o.getOptionText(),
                            o.getIsCorrect()
                    ))
                    .toList();
        }

        // --- SHORT ANSWER ---
        if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
            correctAnswers = q.getShortAnswerOptions().stream()
                    .map(o -> new ShortAnswerOptionResponse(o.getId(), o.getAnswerText()))
                    .toList();
        }

        // --- User answer ---
        if (answer != null) {
            selectedOptionIds = answer.getSelectedOptions().stream()
                    .map(QuestionOption::getId)
                    .toList();

            answeredText = answer.getAnswerText();
        }

        boolean isCorrect = answer != null && Boolean.TRUE.equals(answer.getIsCorrect());

        // 🔥 LOGIC ĐIỂM
        Double score = null;

        if (!Boolean.TRUE.equals(quiz.getAutoScore())) {
            // chỉ hiện khi quiz KHÔNG auto score
            score = q.getScore() != null ? q.getScore() : 0.0;
        }

        return new QuizAttemptAnswerResponse(
                q.getId(),
                q.getContent(),
                q.getQuestionType(),
                q.getMediaObjectKey() != null ? minioChannel.getPresignedUrlSafe(q.getMediaObjectKey(),3600) : null,
                q.getMediaType(),
                options,
                correctAnswers,
                selectedOptionIds,
                answeredText,
                isCorrect,
                score
        );
    }

    private QuizResultResponse toQuizResultResponse(QuizAttempt attempt) {

        int totalQuestions = getQuestionsForAttempt(attempt).size();

        return QuizResultResponse.builder()
                .attemptId(attempt.getId())
                .correctCount(attempt.getCorrectCount())
                .totalQuestions(totalQuestions)
                .totalScore(attempt.getTotalScore())
                .submittedAt(attempt.getSubmittedAt())
                .submittedByName(attempt.getCreator().getUsername())
                .submittedByEmail(attempt.getCreator().getEmail())
                .build();
    }

    public String normalizeKeyword(String str) {
        if (str == null) return null;

        return str
                .replaceAll("\\s+", " ") // nhiều space → 1 space
                .trim();                // bỏ space đầu cuối
    }

    public boolean hasAccent(String str) {
        if (str == null) return false;

        // normalize về dạng decomposed
        String normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);

        // nếu có ký tự dấu (diacritics) thì return true
        return normalized.matches(".*\\p{InCombiningDiacriticalMarks}+.*");
    }


}
