package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.*;

import com.example.Auto_Grade.dto.res.*;
import com.example.Auto_Grade.entity.*;

import com.example.Auto_Grade.entity.Class;
import com.example.Auto_Grade.enums.*;
import com.example.Auto_Grade.integration.minio.MinioChannel;
import com.example.Auto_Grade.repository.*;

import com.example.Auto_Grade.service.QuizService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;


@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final AccountRepository accountRepository;
    private final ClassRepository classRepository;
    private final QuestionRepository questionRepository;
    private final MinioChannel minioChannel;
    private final QuizAttemptRepository attemptRepository;
    private final QuizQuestionConfigRepository configRepository;
    private final CategoryQuestionRepository categoryQuestionRepository;
    private final GroupQuestionRepository groupQuestionRepository;
    private final QuizAnswerRepository quizAnswerRepository;


    // ─────────────── helpers ───────────────

    private Account getCurrentAccount() {
        Long id = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tài khoản không tồn tại"));
    }

    private void validateOwner(Quiz quiz, Account account) {
        if (!quiz.getCreator().getId().equals(account.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác với quiz này");
        }
    }

    public String generateQuizCode() {

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        int maxRetry = 5;

        for (int attempt = 0; attempt < maxRetry; attempt++) {

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < 10; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }

            String code = sb.toString();

            if (!quizRepository.existsByQuizCode(code)) {
                return code;
            }
        }

        throw new RuntimeException("Lỗi. Vui lòng thử lại.");
    }


    @Override
    public void createQuiz(QuizRequest request) {
        String code = generateQuizCode();

        validateTime(request);

        Account creator = getCurrentAccount();

        boolean isRandom = Boolean.TRUE.equals(request.getIsRandom());

        // Validate theo mode
        if (isRandom) {
            if (request.getRandomConfigs() == null || request.getRandomConfigs().isEmpty()) {
                throw new IllegalArgumentException("Cần cấu hình câu hỏi cho quizz.");
            }
        } else {
            if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
                throw new IllegalArgumentException("Danh sách câu hỏi đang để trống.");
            }
        }

        Quiz quiz = Quiz.builder()
                .title(request.getTitle().trim())
                .quizCode(code)
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .maxAttempts(request.getMaxAttempts())
                .totalScore(request.getTotalScore())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isRandom(isRandom)
                .allowReview(request.getAllowReview() != null ? request.getAllowReview() : false)
                .autoScore(request.getAutoScore() != null ? request.getAutoScore() : true)
                .status(request.getQuizStatus() != null ? request.getQuizStatus() : QuizStatus.DRAFT)
                .accessType(
                        request.getQuizAccessType() != null
                                ? request.getQuizAccessType()
                                : com.example.Auto_Grade.enums.QuizAccessType.PUBLIC
                )
                .creator(creator)
                .build();

        // set class
        if (request.getClassId() != null && !request.getClassId().isEmpty()) {
            List<Class> classes = classRepository.findAllById(request.getClassId());
            quiz.setClasses(classes);
        }

        quizRepository.save(quiz);

        if (isRandom) {
            applyRandomConfigs(quiz, request.getRandomConfigs());
        } else {
            List<Question> questions = new ArrayList<>();
            int index = 1;

            for (QuizQuestionRequest qReq : request.getQuestions()) {

                boolean hasContent = qReq.getContent() != null && !qReq.getContent().trim().isEmpty();
                boolean hasMedia = qReq.getMediaObjectKey() != null && !qReq.getMediaObjectKey().trim().isEmpty();

                // validate content
                if (!hasContent && !hasMedia) {
                    throw new IllegalArgumentException("Câu hỏi phải có nội dung hoặc file ở câu " + index);
                }

                // validate type
                if (qReq.getQuestionType() == null) {
                    throw new IllegalArgumentException("Loại câu hỏi không được để trống ở câu " + index);
                }

                // 🔥 validate score nếu KHÔNG autoScore
                if (!quiz.getAutoScore()) {
                    if (qReq.getScore() == null || qReq.getScore() <= 0) {
                        throw new IllegalArgumentException("Điểm của câu hỏi phải lớn hơn 0 ở câu " + index);
                    }
                }

                // build question
                Question question = Question.builder()
                        .content(hasContent ? qReq.getContent().trim() : null)
                        .questionType(qReq.getQuestionType())
                        .mediaObjectKey(hasMedia ? qReq.getMediaObjectKey() : null)
                        .mediaType(qReq.getMediaType())
                        .score(quiz.getAutoScore() ? null : qReq.getScore()) // 🔥 KEY
                        .quiz(quiz)
                        .build();

                // apply options / answers
                applyQuizQuestionDetails(question, qReq, index);

                questions.add(question);
                index++;
            }

            // 🔥 validate tổng điểm (CHỈ khi không autoScore)
            if (!quiz.getAutoScore()) {
                BigDecimal sum = questions.stream()
                        .map(q -> BigDecimal.valueOf(q.getScore()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal total = BigDecimal.valueOf(quiz.getTotalScore());

                if (sum.compareTo(total) != 0) {
                    throw new IllegalArgumentException(
                            "Tổng điểm các câu (" + sum + ") phải bằng tổng điểm bài (" + total + ")"
                    );
                }
            }

            questionRepository.saveAll(questions);
        }
    }

    private void applyQuizQuestionDetails(Question question,
                                          QuizQuestionRequest request,
                                          int index) {

        try {
            QuestionType type = request.getQuestionType();

            // TRẮC NGHIỆM
            if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE) {

                if (request.getOptions() == null || request.getOptions().size() < 2) {
                    throw new IllegalArgumentException("Cần ít nhất 2 đáp án");
                }

                Set<String> seen = new HashSet<>();

                for (QuestionOptionRequest opt : request.getOptions()) {

                    if (opt.getOptionText() == null || opt.getOptionText().trim().isEmpty()) {
                        throw new IllegalArgumentException("Một đáp án đang để trống");
                    }

                    String value = opt.getOptionText().trim();

                    if (!seen.add(value)) {
                        throw new IllegalArgumentException("Đáp án bị trùng");
                    }
                }

                if (type == QuestionType.SINGLE_CHOICE) {
                    long correct = request.getOptions().stream()
                            .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                            .count();

                    if (correct != 1) {
                        throw new IllegalArgumentException("Chỉ được có 1 đáp án đúng");
                    }
                }

                if (type == QuestionType.MULTIPLE_CHOICE) {
                    boolean hasCorrect = request.getOptions().stream()
                            .anyMatch(o -> Boolean.TRUE.equals(o.getIsCorrect()));

                    if (!hasCorrect) {
                        throw new IllegalArgumentException("Phải có ít nhất 1 đáp án đúng");
                    }
                }

                for (QuestionOptionRequest opt : request.getOptions()) {
                    question.getOptions().add(
                            QuestionOption.builder()
                                    .optionText(opt.getOptionText().trim())
                                    .isCorrect(Boolean.TRUE.equals(opt.getIsCorrect()))
                                    .question(question)
                                    .build()
                    );
                }
            }

            // SHORT ANSWER
            else if (type == QuestionType.SHORT_ANSWER) {

                if (request.getCorrectAnswers() == null || request.getCorrectAnswers().isEmpty()) {
                    throw new IllegalArgumentException("Phải có ít nhất 1 đáp án");
                }

                Set<String> seen = new HashSet<>();

                for (ShortAnswerOptionRequest ans : request.getCorrectAnswers()) {

                    if (ans.getAnswer() == null || ans.getAnswer().trim().isEmpty()) {
                        throw new IllegalArgumentException("Một đáp án đang để trống");
                    }

                    String value = ans.getAnswer().trim();

                    if (!seen.add(value)) {
                        throw new IllegalArgumentException("Đáp án bị trùng");
                    }

                    question.getShortAnswerOptions().add(
                            ShortAnswerOption.builder()
                                    .answerText(normalizeKeyword(value))
                                    .question(question)
                                    .build()
                    );
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage() + " ở câu " + index);
        }
    }

    private void applyRandomConfigs(Quiz quiz, List<QuizQuestionConfigRequest> randomConfigs) {
        Long creatorId = quiz.getCreator().getId(); // ← lấy từ quiz

        for (QuizQuestionConfigRequest cfg : randomConfigs) {
            long available = questionRepository.countByFilters(
                    creatorId,                      // ← thêm
                    cfg.getCategoryQuestionId(),
                    cfg.getGroupQuestionId()
            );

            if (available < cfg.getQuantity()) {
                String categoryName = cfg.getCategoryQuestionId() != null
                        ? categoryQuestionRepository.findById(cfg.getCategoryQuestionId())
                        .map(CategoryQuestion::getName)
                        .orElse("id=" + cfg.getCategoryQuestionId())
                        : "tất cả";

                String groupName = cfg.getGroupQuestionId() != null
                        ? groupQuestionRepository.findById(cfg.getGroupQuestionId())
                        .map(GroupQuestion::getName)
                        .orElse("id=" + cfg.getGroupQuestionId())
                        : "tất cả";

                throw new IllegalArgumentException(String.format(
                        "Không đủ câu hỏi ở danh mục '%s', nhóm '%s'. Cần %d câu nhưng chỉ có %d câu.",
                        categoryName, groupName, cfg.getQuantity(), available
                ));
            }
        }

        List<QuizQuestionConfig> configs = randomConfigs.stream()
                .map(cfg -> QuizQuestionConfig.builder()
                        .quiz(quiz)
                        .categoryQuestionId(cfg.getCategoryQuestionId())
                        .groupQuestionId(cfg.getGroupQuestionId())
                        .quantity(cfg.getQuantity())
                        .build())
                .toList();

        configRepository.saveAll(configs);
    }

    @Override
    @Transactional
    public void deleteQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy quiz với id: " + quizId));
        validateOwner(quiz, getCurrentAccount());
        quizRepository.delete(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<QuizResponse> getQuiz(
            String title,
            Long classId,
            QuizStatus status,
            QuizAccessType quizAccessType,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

        Account account = getCurrentAccount();

        String keyword = normalizeKeyword(title);

        Page<Quiz> quizPage;

        if (hasAccent(keyword)) {
            quizPage = quizRepository.searchWithAccent(account.getId(), keyword, status, quizAccessType, classId, pageable);
        } else {
            quizPage = quizRepository.searchQuizzes(account.getId(), keyword, status, quizAccessType, classId, pageable);
        }

        MetaResponse meta = MetaResponse.builder()
                .totalItems(quizPage.getTotalElements())
                .itemCount(quizPage.getNumberOfElements())
                .itemsPerPage(quizPage.getSize())
                .totalPages(quizPage.getTotalPages())
                .currentPage(quizPage.getNumber() + 1)
                .build();

        return PagingResponse.<QuizResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy danh sách bài kiểm tra thành công")
                .data(quizPage.map(this::mapToResponse).getContent())
                .meta(meta)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizDetail(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài kiểm tra với id: " + quizId));

        validateOwner(quiz, getCurrentAccount());

        List<QuestionResponse> questionResponses = quiz.getQuestions().stream()
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .content(q.getContent())
                        .questionType(q.getQuestionType())
                        .score(q.getScore())
                        .mediaUrl(minioChannel.getPresignedUrlSafe(q.getMediaObjectKey(), 3600))
                        .mediaType(q.getMediaType())
                        .mediaObjectKey(q.getMediaObjectKey())
                        .options(q.getOptions().stream()
                                .map(opt -> QuestionOptionResponse.builder()
                                        .id(opt.getId())
                                        .optionText(opt.getOptionText())
                                        .isCorrect(opt.getIsCorrect())
                                        .build())
                                .toList())
                        .correctAnswers(q.getShortAnswerOptions().stream()
                                .map(ans -> ShortAnswerOptionResponse.builder()
                                        .id(ans.getId())
                                        .answer(ans.getAnswerText())
                                        .build())
                                .toList())
                        .build())
                .toList();

        List<QuizClassResponse> classResponses = quiz.getClasses().stream()
                .map(c -> QuizClassResponse.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .build())
                .toList();

        List<QuizQuestionConfigResponse> configResponses = configRepository.findByQuizId(quizId)
                .stream()
                .map(c -> {
                    String categoryName = c.getCategoryQuestionId() != null
                            ? categoryQuestionRepository.findById(c.getCategoryQuestionId())
                            .map(CategoryQuestion::getName)
                            .orElse(null)
                            : null;

                    String groupName = c.getGroupQuestionId() != null
                            ? groupQuestionRepository.findById(c.getGroupQuestionId())
                            .map(GroupQuestion::getName)
                            .orElse(null)
                            : null;

                    return QuizQuestionConfigResponse.builder()
                            .id(c.getId())
                            .categoryQuestionId(c.getCategoryQuestionId())
                            .categoryQuestionName(categoryName)
                            .groupQuestionId(c.getGroupQuestionId())
                            .groupQuestionName(groupName)
                            .quantity(c.getQuantity())
                            .build();
                })
                .toList();

        return QuizDetailResponse.builder()
                .id(quiz.getId())
                .quizCode(quiz.getQuizCode())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .totalScore(quiz.getTotalScore())
                .durationMinutes(quiz.getDurationMinutes())
                .maxAttempts(quiz.getMaxAttempts())
                .startTime(quiz.getStartTime())
                .endTime(quiz.getEndTime())
                .allowReview(quiz.getAllowReview())
                .quizStatus(quiz.getStatus())
                .quizAccessType(quiz.getAccessType())
                .autoScore(quiz.getAutoScore())
                .questions(questionResponses)
                .classes(classResponses)
                .isRandom(quiz.getIsRandom())
                .randomConfigs(configResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<QuizForMemberResponse> getQuizByClassId(Long classId, int page, int size) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp"));

        // 🔥 check member
        boolean isMember = clazz.getMembers().stream()
                .anyMatch(m ->
                        m.getAccount().getId().equals(accountId)
                                && m.getStatus() == MemberStatus.APPROVED
                );

        if (!isMember) {
            throw new AccessDeniedException("Bạn không phải thành viên lớp");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

        Page<Quiz> quizPage = quizRepository.findQuizForMember(classId, pageable);

        List<Quiz> quizzes = quizPage.getContent();

        //Lấy list quizId
        List<Long> quizIds = quizzes.stream()
                .map(Quiz::getId)
                .toList();

        //Tránh lỗi IN ()
        Set<Long> submittedQuizIds = quizIds.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(
                attemptRepository.findSubmittedQuizIds(
                        accountId,
                        quizIds,
                        AttemptStatus.SUBMITTED
                )
        );

        //Map response
        List<QuizForMemberResponse> data = quizzes.stream()
                .map(q -> QuizForMemberResponse.builder()
                        .id(q.getId())
                        .quizTitle(q.getTitle())
                        .quizDescription(q.getDescription())
                        .isSubmitted(submittedQuizIds.contains(q.getId()))
                        .build()
                )
                .toList();

        MetaResponse meta = MetaResponse.builder()
                .totalItems(quizPage.getTotalElements())
                .itemCount(quizPage.getNumberOfElements())
                .itemsPerPage(quizPage.getSize())
                .totalPages(quizPage.getTotalPages())
                .currentPage(quizPage.getNumber() + 1)
                .build();

        return PagingResponse.<QuizForMemberResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy danh sách quiz cho sinh viên thành công")
                .data(data)
                .meta(meta)
                .build();
    }

    @Override
    @Transactional
    public void updateQuiz(Long quizId, QuizRequest request) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy bài kiểm tra với id: " + quizId));

        validateOwner(quiz, getCurrentAccount());

        boolean isDraft = quiz.getStatus() == QuizStatus.DRAFT;

        // ── Metadata: cả DRAFT lẫn PUBLISHED đều được chỉnh sửa ───────────────
        validateTime(request);

        quiz.setTitle(request.getTitle().trim());
        quiz.setDescription(request.getDescription());
        quiz.setDurationMinutes(request.getDurationMinutes());
        quiz.setMaxAttempts(request.getMaxAttempts());
        quiz.setTotalScore(request.getTotalScore());
        quiz.setStartTime(request.getStartTime());
        quiz.setEndTime(request.getEndTime());
        quiz.setAllowReview(request.getAllowReview() != null ? request.getAllowReview() : false);
        quiz.setStatus(request.getQuizStatus() != null ? request.getQuizStatus() : QuizStatus.DRAFT);
        quiz.setAccessType(request.getQuizAccessType() != null
                ? request.getQuizAccessType()
                : QuizAccessType.PUBLIC);

        if (request.getClassId() != null && !request.getClassId().isEmpty()) {
            List<Class> classes = classRepository.findAllById(request.getClassId());
            quiz.setClasses(classes);
        } else {
            quiz.setClasses(new ArrayList<>());
        }

        // ── Questions: chỉ DRAFT mới được chỉnh sửa ───────────────────────────
        if (!isDraft) {
            // PUBLISHED → bỏ qua hoàn toàn, không throw, không chạm vào questions
            quizRepository.save(quiz);
            return;
        }

        quiz.setAutoScore(request.getAutoScore() != null ? request.getAutoScore() : true);

        boolean isRandom = Boolean.TRUE.equals(request.getIsRandom());
        quiz.setIsRandom(isRandom);

        if (isRandom) {
            if (request.getRandomConfigs() == null || request.getRandomConfigs().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng cấu hình câu hỏi.");
            }
            // Xoá questions thủ công nếu có (chuyển từ thủ công sang random)
            quiz.getQuestions().clear();
            questionRepository.flush();

            // Xoá config cũ, lưu config mới
            configRepository.deleteByQuizId(quizId);
            applyRandomConfigs(quiz, request.getRandomConfigs());

        } else {
            if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập danh sách câu hỏi.");
            }

            // Xoá config random nếu có (chuyển từ random sang thủ công)
            configRepository.deleteByQuizId(quizId);

            quiz.getQuestions().clear();
            questionRepository.flush();

            List<Question> newQuestions = new ArrayList<>();
            int index = 1;

            for (QuizQuestionRequest qReq : request.getQuestions()) {
                boolean hasContent = qReq.getContent() != null && !qReq.getContent().trim().isEmpty();
                boolean hasMedia = qReq.getMediaObjectKey() != null
                        && !qReq.getMediaObjectKey().trim().isEmpty();

                if (!hasContent && !hasMedia) {
                    throw new IllegalArgumentException(
                            "Câu hỏi phải có nội dung hoặc file ở câu " + index);
                }
                if (qReq.getQuestionType() == null) {
                    throw new IllegalArgumentException(
                            "Loại câu hỏi không được để trống ở câu " + index);
                }
                if (!quiz.getAutoScore()) {
                    if (qReq.getScore() == null || qReq.getScore() <= 0) {
                        throw new IllegalArgumentException(
                                "Điểm của câu hỏi phải lớn hơn 0 ở câu " + index);
                    }
                }

                Question question = Question.builder()
                        .content(hasContent ? qReq.getContent().trim() : null)
                        .questionType(qReq.getQuestionType())
                        .mediaObjectKey(hasMedia ? qReq.getMediaObjectKey() : null)
                        .mediaType(qReq.getMediaType())
                        .score(quiz.getAutoScore() ? null : qReq.getScore())
                        .quiz(quiz)
                        .build();

                applyQuizQuestionDetails(question, qReq, index);
                newQuestions.add(question);
                index++;
            }

            if (!quiz.getAutoScore()) {
                BigDecimal sum = newQuestions.stream()
                        .map(q -> BigDecimal.valueOf(q.getScore()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal total = BigDecimal.valueOf(quiz.getTotalScore());
                if (sum.compareTo(total) != 0) {
                    throw new IllegalArgumentException(
                            "Tổng điểm các câu (" + sum + ") phải bằng tổng điểm bài (" + total + ")");
                }
            }

            questionRepository.saveAll(newQuestions);
        }

        quizRepository.save(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDetail getQuizDetailForMember(Long quizId) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy quiz"));

        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new AccessDeniedException("Quiz chưa được công bố");
        }

        //  Kiểm tra quyền: phải là member của class chứa quiz (nếu PRIVATE)
        if (quiz.getAccessType() == QuizAccessType.PRIVATE) {

            boolean isMember = quiz.getClasses().stream()
                    .flatMap(c -> c.getMembers().stream())
                    .anyMatch(m ->
                            m.getAccount().getId().equals(accountId)
                                    && m.getStatus() == MemberStatus.APPROVED
                    );

            if (!isMember) {
                throw new AccessDeniedException("Bạn không có quyền xem quiz này");
            }
        }

        return QuizDetail.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .durationMinutes(quiz.getDurationMinutes())
                .maxAttempts(quiz.getMaxAttempts())
                .startTime(quiz.getStartTime())
                .endTime(quiz.getEndTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDetail getQuizByCode(String quizCode) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Quiz quiz = quizRepository.findByQuizCode(quizCode)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy quiz"));

        // ❌ chỉ cho phép quiz published
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new AccessDeniedException("Quiz chưa được công bố");
        }

        // ❌ kiểm tra quyền nếu PRIVATE
        if (quiz.getAccessType() == QuizAccessType.PRIVATE) {

            boolean isMember = quiz.getClasses().stream()
                    .flatMap(c -> c.getMembers().stream())
                    .anyMatch(m ->
                            m.getAccount().getId().equals(accountId)
                                    && m.getStatus() == MemberStatus.APPROVED
                    );

            if (!isMember) {
                throw new AccessDeniedException("Bạn không có quyền tham gia quiz này");
            }
        }

        return QuizDetail.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .durationMinutes(quiz.getDurationMinutes())
                .maxAttempts(quiz.getMaxAttempts())
                .startTime(quiz.getStartTime())
                .endTime(quiz.getEndTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizStatisticsResponse getQuizStatistics(Long quizId) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy quiz"));

        // 🔐 chỉ chủ quiz xem
        if (!quiz.getCreator().getId().equals(accountId)) {
            throw new AccessDeniedException("Không có quyền xem thống kê");
        }

        List<QuizAttempt> attempts = attemptRepository
                .findByQuizIdAndStatus(quizId, AttemptStatus.SUBMITTED);

        // =========================
        // 🎯 EMPTY
        // =========================
        if (attempts.isEmpty()) {
            return QuizStatisticsResponse.builder()
                    .totalAttempts(0)
                    .averageScore(0)
                    .averageTime("0 giây")
                    .randomQuestions(quiz.getIsRandom())
                    .excellent(0)
                    .good(0)
                    .average(0)
                    .weak(0)
                    .build();
        }

        long totalAttempts = attempts.size();

        // =========================
        // 🎯 AVG SCORE
        // =========================
        double avgScore = attempts.stream()
                .mapToDouble(a -> a.getTotalScore() != null ? a.getTotalScore() : 0)
                .average()
                .orElse(0);

        // =========================
        // 🎯 AVG TIME
        // =========================
        long avgSeconds = (long) attempts.stream()
                .filter(a -> a.getStartedAt() != null && a.getSubmittedAt() != null)
                .mapToLong(a -> java.time.Duration
                        .between(a.getStartedAt(), a.getSubmittedAt())
                        .getSeconds())
                .average()
                .orElse(0);

        String avgTime = formatDuration(avgSeconds);

        // =========================
        // 🎯 SCORE DISTRIBUTION
        // =========================
        long excellent = 0, good = 0, average = 0, weak = 0;

        for (QuizAttempt a : attempts) {
            double score = a.getTotalScore() != null ? a.getTotalScore() : 0;

            if (score >= 8) excellent++;
            else if (score >= 6.5) good++;
            else if (score >= 5) average++;
            else weak++;
        }

        return QuizStatisticsResponse.builder()
                .totalAttempts(totalAttempts)
                .averageScore(round(avgScore))
                .randomQuestions(quiz.getIsRandom())
                .averageTime(avgTime)
                .excellent(excellent)
                .good(good)
                .average(average)
                .weak(weak)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionStatisticsResponse> getQuestionStatistics(Long quizId) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy quiz"));

        if (!quiz.getCreator().getId().equals(accountId)) {
            throw new AccessDeniedException("Không có quyền xem thống kê");
        }

        if (Boolean.TRUE.equals(quiz.getIsRandom())) {
            throw new IllegalStateException(
                    "Thống kê chi tiết câu hỏi không hỗ trợ quiz ngẫu nhiên");
        }

        // Tổng số attempt đã nộp
        long totalAttempts = attemptRepository
                .countByQuizIdAndStatus(quizId, AttemptStatus.SUBMITTED);

        if (totalAttempts == 0) {
            return buildEmptyStats(quiz);
        }

        // Fetch toàn bộ answers 1 lần — tránh N+1
        List<QuizAnswer> allAnswers = quizAnswerRepository
                .findAllAnswersByQuizIdAndStatus(quizId, AttemptStatus.SUBMITTED);

        // Group: questionId → list QuizAnswer
        Map<Long, List<QuizAnswer>> byQuestion = allAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));

        List<QuestionStatisticsResponse> result = new ArrayList<>();

        for (Question question : quiz.getQuestions()) {

            List<QuizAnswer> answers = byQuestion
                    .getOrDefault(question.getId(), Collections.emptyList());

            long answered = answers.stream()
                    .filter(this::isAnswered)
                    .count();

            long skipped = totalAttempts - answered;

            long correct = answers.stream()
                    .filter(this::isAnswered)
                    .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                    .count();

            long wrong = answered - correct;

            double correctPercent = totalAttempts > 0
                    ? Math.floor((correct * 100.0 / totalAttempts) * 100) / 100
                    : 0;

            double skipPercent = totalAttempts > 0
                    ? Math.floor((skipped * 100.0 / totalAttempts) * 100) / 100
                    : 0;

            double wrongPercent = totalAttempts > 0
                    ? Math.floor((wrong * 100.0 / totalAttempts) * 100) / 100
                    : 0;

            List<OptionStatisticsResponse> optionStats = null;

            if (question.getQuestionType() == QuestionType.SINGLE_CHOICE) {
                optionStats = buildOptionStats(question, answers, totalAttempts);
            }

            result.add(QuestionStatisticsResponse.builder()
                    .questionId(question.getId())
                    .content(question.getContent())
                    .mediaType(question.getMediaType())
                    .mediaUrl(minioChannel.getPresignedUrlSafe(question.getMediaObjectKey(), 3600))
                    .questionType(question.getQuestionType())
                    .correctCount(correct)
                    .wrongCount(wrong)
                    .skippedCount(skipped)
                    .correctPercent(correctPercent)
                    .skipPercent(skipPercent)
                    .wrongPercent(wrongPercent)
                    .optionStats(optionStats)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public void exportResultsToExcel(Long quizId, HttpServletResponse response) {

        Long accountId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy quiz"));

        if (!quiz.getCreator().getId().equals(accountId)) {
            throw new AccessDeniedException("Bạn không có quyền xuất dữ liệu");
        }

        List<QuizAttempt> attempts = attemptRepository
                .findByQuizIdAndStatus(quizId, AttemptStatus.SUBMITTED);

        // ── Setup response header ────────────────────────────────
        String fileName = "ket-qua-" + quizId + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // ── Tạo workbook ─────────────────────────────────────────
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Kết quả");

            // ── Style header ─────────────────────────────────────
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            // ── Style data ───────────────────────────────────────
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(
                    createHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm:ss"));
            dateStyle.setAlignment(HorizontalAlignment.CENTER);

            // ── Header row ───────────────────────────────────────
            String[] headers = {
                    "STT", "Họ tên", "Email",
                    "Số câu đúng", "Tổng số câu", "Điểm", "Thời điểm nộp"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Data rows ────────────────────────────────────────
            int rowIndex = 1;
            int totalQuestions;

            if (Boolean.TRUE.equals(quiz.getIsRandom())) {
                // Random quiz: tổng quantity từ các config
                totalQuestions = configRepository.findByQuizId(quiz.getId())
                        .stream()
                        .mapToInt(QuizQuestionConfig::getQuantity)
                        .sum();
            } else {
                // Manual quiz: đếm trực tiếp
                totalQuestions = quiz.getQuestions().size();
            }

            for (QuizAttempt attempt : attempts) {

                Row row = sheet.createRow(rowIndex++);

                // STT
                Cell sttCell = row.createCell(0);
                sttCell.setCellValue(rowIndex - 1);
                sttCell.setCellStyle(dataStyle);

                // Họ tên
                Cell nameCell = row.createCell(1);
                nameCell.setCellValue(attempt.getCreator().getUsername());
                nameCell.setCellStyle(dataStyle);

                // Email
                Cell emailCell = row.createCell(2);
                emailCell.setCellValue(attempt.getCreator().getEmail());
                emailCell.setCellStyle(dataStyle);

                // Số câu đúng
                Cell correctCell = row.createCell(3);
                correctCell.setCellValue(attempt.getCorrectCount() != null ? attempt.getCorrectCount() : 0);
                correctCell.setCellStyle(dataStyle);

                // Tổng câu
                Cell totalCell = row.createCell(4);
                totalCell.setCellValue(totalQuestions);
                totalCell.setCellStyle(dataStyle);

                // Điểm
                Cell scoreCell = row.createCell(5);
                scoreCell.setCellValue(attempt.getTotalScore() != null ? attempt.getTotalScore() : 0);
                scoreCell.setCellStyle(dataStyle);

                // Thời điểm nộp
                Cell submittedCell = row.createCell(6);
                if (attempt.getSubmittedAt() != null) {
                    submittedCell.setCellValue(
                            java.util.Date.from(attempt.getSubmittedAt()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toInstant()));
                    submittedCell.setCellStyle(dateStyle);
                }
            }

            // ── Auto resize columns ───────────────────────────────
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }catch (IOException e) {
            throw new IllegalArgumentException("Lỗi khi xuất file Excel: " + e.getMessage());
        }
    }

    private List<OptionStatisticsResponse> buildOptionStats(Question question,
                                                            List<QuizAnswer> answers,
                                                            long totalAttempts) {
        Map<Long, Long> countByOptionId = answers.stream()
                .filter(a -> !a.getSelectedOptions().isEmpty())
                .collect(Collectors.groupingBy(
                        a -> a.getSelectedOptions().getFirst().getId(),
                        Collectors.counting()
                ));

        long base = totalAttempts == 0 ? 1 : totalAttempts; // ← chia cho tổng bài nộp

        return question.getOptions().stream()
                .map(opt -> {
                    long count  = countByOptionId.getOrDefault(opt.getId(), 0L);
                    double pct = Math.floor((count * 100.0 / base) * 100.0) / 100.0;
                    return OptionStatisticsResponse.builder()
                            .optionId(opt.getId())
                            .optionText(opt.getOptionText())
                            .isCorrect(opt.getIsCorrect())
                            .chosenCount(count)
                            .chosenPercent(pct)
                            .build();
                })
                .toList();
    }

    private List<QuestionStatisticsResponse> buildEmptyStats(Quiz quiz) {

        List<QuestionStatisticsResponse> result = new ArrayList<>();

        for (Question q : quiz.getQuestions()) {

            boolean hasOptions = q.getQuestionType() == QuestionType.SINGLE_CHOICE;

            List<OptionStatisticsResponse> optionStats = hasOptions
                    ? q.getOptions().stream()
                    .map(opt -> OptionStatisticsResponse.builder()
                            .optionId(opt.getId())
                            .optionText(opt.getOptionText())
                            .isCorrect(opt.getIsCorrect())
                            .chosenCount(0)
                            .chosenPercent(0.0)
                            .build())
                    .toList()
                    : null;

            result.add(QuestionStatisticsResponse.builder()
                    .questionId(q.getId())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .mediaType(q.getMediaType())
                    .mediaUrl(minioChannel.getPresignedUrlSafe(q.getMediaObjectKey(), 3600))
                    .correctCount(0)
                    .wrongCount(0)
                    .skippedCount(0)
                    .correctPercent(0)
                    .skipPercent(0)
                    .wrongPercent(0)
                    .optionStats(optionStats)
                    .build());
        }
        return result;
    }

    private QuizResponse mapToResponse(Quiz quiz) {
        int questionCount;

        if (Boolean.TRUE.equals(quiz.getIsRandom())) {
            // Random quiz: tổng quantity từ các config
            questionCount = configRepository.findByQuizId(quiz.getId())
                    .stream()
                    .mapToInt(QuizQuestionConfig::getQuantity)
                    .sum();
        } else {
            // Manual quiz: đếm trực tiếp
            questionCount = quiz.getQuestions().size();
        }

        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .durationMinutes(quiz.getDurationMinutes())
                .maxAttempts(quiz.getMaxAttempts())
                .createdAt(quiz.getCreatedAt())
                .questionCount(questionCount)
                .quizAccessType(quiz.getAccessType())
                .quizStatus(quiz.getStatus())
                .build();
    }

    public String normalizeKeyword(String str) {
        if (str == null) return null;

        return str
                .replaceAll("\\s+", " ") // nhiều space → 1 space
                .trim();                // bỏ space đầu cuối
    }

    private boolean isAnswered(QuizAnswer a) {
        if (a.getQuestion().getQuestionType() == QuestionType.SHORT_ANSWER) {
            return a.getAnswerText() != null && !a.getAnswerText().isBlank();
        }

        return a.getSelectedOptions() != null && !a.getSelectedOptions().isEmpty();
    }

    public boolean hasAccent(String str) {
        if (str == null) return false;

        // normalize về dạng decomposed
        String normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);

        // nếu có ký tự dấu (diacritics) thì return true
        return normalized.matches(".*\\p{InCombiningDiacriticalMarks}+.*");
    }

    private void validateTime(QuizRequest request) {

        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();

        // nếu có đủ cả 2 thì mới validate
        if (start != null && end != null) {
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
            }
        }

        // check start không ở quá khứ (nếu có nhập)
        if (end != null && end.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian kết thúc không nhỏ hơn thời điểm hiện tại");
        }
    }

    private String formatDuration(long seconds) {

        if (seconds < 0) {
            return "0 giây";
        }

        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        StringBuilder sb = new StringBuilder();

        if (h > 0) sb.append(h).append(" giờ ");
        if (m > 0) sb.append(m).append(" phút ");
        if (s > 0 || sb.isEmpty()) sb.append(s).append(" giây");

        return sb.toString().trim();
    }


}

