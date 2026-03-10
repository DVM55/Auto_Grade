package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.QuestionOptionRequest;
import com.example.Auto_Grade.dto.req.QuestionBankRequest;
import com.example.Auto_Grade.dto.req.ShortAnswerOptionRequest;

import com.example.Auto_Grade.dto.res.QuestionBankResponse;
import com.example.Auto_Grade.dto.res.QuestionOptionResponse;
import com.example.Auto_Grade.dto.res.ShortAnswerOptionResponse;
import com.example.Auto_Grade.entity.*;
import com.example.Auto_Grade.enums.MediaType;
import com.example.Auto_Grade.enums.QuestionType;
import com.example.Auto_Grade.integration.minio.MinioChannel;
import com.example.Auto_Grade.repository.*;
import com.example.Auto_Grade.service.QuestionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final GroupQuestionRepository groupQuestionRepository;
    private final CategoryQuestionRepository categoryQuestionRepository;
    private final AccountRepository accountRepository;
    private final MinioChannel minioChannel;

    // ─────────────── helpers ───────────────

    private void validateQuestionOwner(Question question) {
        Long currentId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!question.getCreator().getId().equals(currentId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập/chỉnh sửa câu hỏi này");
        }
    }



    /**
     * Validate và gán options / correctAnswers theo loại câu hỏi.
     * - SINGLE_CHOICE / MULTIPLE_CHOICE → dùng options (text + isCorrect)
     *   SINGLE_CHOICE chỉ được đúng đúng 1 đáp án
     * - SHORT_ANSWER → dùng correctAnswers (danh sách chuỗi, tối thiểu 1)
     */
    private void applyQuestionsDetails(Question question, QuestionBankRequest request) {
        QuestionType type = request.getQuestionType();

        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE) {
            // SINGLE_CHOICE: chỉ 1 đáp án đúng
            if (type == QuestionType.SINGLE_CHOICE) {
                long correctCount = request.getOptions().stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).count();
                if (correctCount != 1) {
                    throw new IllegalArgumentException(
                            "Câu hỏi chọn 1 kết quả chỉ có duy nhất 1 đáp án đúng");
                }
            }

            // Xoá options cũ, thêm mới
            question.getOptions().clear();
            for (QuestionOptionRequest optReq : request.getOptions()) {
                QuestionOption opt = QuestionOption.builder()
                        .optionText(optReq.getOptionText().trim())
                        .isCorrect(Boolean.TRUE.equals(optReq.getIsCorrect()))
                        .question(question)
                        .build();
                question.getOptions().add(opt);
            }
            // Xoá short answer nếu có
            question.getShortAnswerOptions().clear();

        } else if (type == QuestionType.SHORT_ANSWER) {
            // Xoá short answer cũ, thêm mới
            question.getShortAnswerOptions().clear();
            for (ShortAnswerOptionRequest ans : request.getCorrectAnswers()) {
                ShortAnswerOption sao = ShortAnswerOption.builder()
                        .answerText(ans.getAnswer().trim())
                        .question(question)
                        .build();

                question.getShortAnswerOptions().add(sao);
            }
            // Xoá options trắc nghiệm nếu có
            question.getOptions().clear();
        }
    }

    @Override
    @Transactional
    public void updateQuestion(Long questionId, QuestionBankRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy câu hỏi với id: " + questionId));

        question.setContent(request.getContent());
        question.setQuestionType(request.getQuestionType());
        question.setMediaObjectKey(request.getMediaObjectKey());
        question.setMediaContentType(request.getMediaContentType());

        if (request.getGroupQuestionId() != null) {
            GroupQuestion group = groupQuestionRepository.findById(request.getGroupQuestionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy nhóm câu hỏi với id: " + request.getGroupQuestionId()));
            question.setGroupQuestion(group);
        } else {
            question.setGroupQuestion(null);
        }

        if (request.getCategoryQuestionId() != null) {
            CategoryQuestion category = categoryQuestionRepository.findById(request.getCategoryQuestionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy danh mục câu hỏi với id: " + request.getCategoryQuestionId()));
            question.setCategoryQuestion(category);
        } else {
            question.setCategoryQuestion(null);
        }

        applyQuestionsDetails(question, request);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy câu hỏi với id: " + questionId));
        validateQuestionOwner(question);
        questionRepository.delete(question);
    }

    @Override
    @Transactional
    public void createQuestionBank(List<QuestionBankRequest> requests) {

        Account creator = getCurrentAccount();

        for (QuestionBankRequest request : requests) {

            GroupQuestion group = request.getGroupQuestionId() == null ? null :
                    groupQuestionRepository.findById(request.getGroupQuestionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Không tìm thấy nhóm câu hỏi với id: " + request.getGroupQuestionId()));

            CategoryQuestion category = request.getCategoryQuestionId() == null ? null :
                    categoryQuestionRepository.findById(request.getCategoryQuestionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Không tìm thấy danh mục câu hỏi với id: " + request.getCategoryQuestionId()));

            Question question = Question.builder()
                    .content(request.getContent().trim())
                    .questionType(request.getQuestionType())
                    .mediaObjectKey(request.getMediaObjectKey())
                    .mediaContentType(request.getMediaContentType())
                    .creator(creator)
                    .groupQuestion(group)
                    .categoryQuestion(category)
                    .build();

            applyQuestionsDetails(question, request);
        }
    }

    @Override
    @Transactional
    public void deleteAllQuestionByCreatorId() {
        Long creatorId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Question> questions = questionRepository.findAllByCreatorId(creatorId);
        questionRepository.deleteAll(questions);
    }

    @Override
    public Page<QuestionBankResponse> getQuestionBank(
            Long categoryId,
            Long groupId,
            int page,
            int size
    ) {

        Long creatorId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Question> questionPage =
                questionRepository.searchQuestionBank(creatorId, categoryId, groupId, pageable);

        return questionPage.map(this::mapToResponse);
    }

    @Override
    public QuestionBankResponse getQuestionBankById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy câu hỏi với id: " + questionId));
        validateQuestionOwner(question);
        return mapToResponse(question);
    }

    private QuestionBankResponse mapToResponse(Question question) {

        return QuestionBankResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .questionType(question.getQuestionType())
                .mediaUrl(minioChannel.getPresignedUrlSafe(question.getMediaObjectKey(), 3600))
                .mediaType(parseMediaType(question.getMediaContentType()))
                .groupQuestionName(
                        question.getGroupQuestion() != null
                                ? question.getGroupQuestion().getName()
                                : null
                )
                .categoryQuestionName(
                        question.getCategoryQuestion() != null
                                ? question.getCategoryQuestion().getName()
                                : null
                )
                .options(
                        question.getOptions() == null ? null :
                                question.getOptions().stream()
                                        .map(o -> QuestionOptionResponse.builder()
                                                .id(o.getId())
                                                .optionText(o.getOptionText())
                                                .isCorrect(o.getIsCorrect())
                                                .build())
                                        .toList()
                )
                .correctAnswers(
                        question.getShortAnswerOptions() == null ? null :
                                question.getShortAnswerOptions().stream()
                                        .map(a -> ShortAnswerOptionResponse.builder()
                                                .id(a.getId())
                                                .answer(a.getAnswerText())
                                                .build())
                                        .toList()
                )
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private MediaType parseMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }

        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }

        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }

        if (contentType.startsWith("audio/")) {
            return MediaType.AUDIO;
        }

        return null;
    }

    private Account getCurrentAccount() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + userId));
    }
}
