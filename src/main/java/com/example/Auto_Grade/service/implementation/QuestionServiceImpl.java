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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Override
    public List<QuestionBankRequest> importWord(MultipartFile file) {

        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            // ghép toàn bộ text trong file Word
            StringBuilder textBuilder = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                textBuilder.append(paragraph.getText()).append("\n");
            }

            String text = textBuilder.toString();

            // regex bắt cả tiếng Anh và tiếng Việt
            Pattern pattern = Pattern.compile(
                    "(Question\\s*\\d+|Câu\\s*hỏi\\s*\\d+):\\s*(.*?)\\s*" +
                            "A[.):]\\s*(.*?)\\s*" +
                            "B[.):]\\s*(.*?)\\s*" +
                            "C[.):]\\s*(.*?)\\s*" +
                            "D[.):]\\s*(.*?)\\s*" +
                            "(?:Correct answer|Đáp án):\\s*([A-D])",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );

            Matcher matcher = pattern.matcher(text);

            List<QuestionBankRequest> questions = new ArrayList<>();

            while (matcher.find()) {

                try {

                    String content = matcher.group(2).trim();

                    String A = matcher.group(3).trim();
                    String B = matcher.group(4).trim();
                    String C = matcher.group(5).trim();
                    String D = matcher.group(6).trim();

                    String correct = matcher.group(7).trim().toUpperCase();

                    List<QuestionOptionRequest> options = List.of(
                            QuestionOptionRequest.builder().optionText(A).isCorrect("A".equals(correct)).build(),
                            QuestionOptionRequest.builder().optionText(B).isCorrect("B".equals(correct)).build(),
                            QuestionOptionRequest.builder().optionText(C).isCorrect("C".equals(correct)).build(),
                            QuestionOptionRequest.builder().optionText(D).isCorrect("D".equals(correct)).build()
                    );

                    QuestionBankRequest question = QuestionBankRequest.builder()
                            .content(content)
                            .questionType(QuestionType.SINGLE_CHOICE)
                            .options(options)
                            .build();

                    questions.add(question);

                } catch (Exception e) {
                    System.out.println("Skip câu hỏi lỗi: " + e.getMessage());
                }
            }

            if (questions.isEmpty()) {
                throw new IllegalArgumentException("File Word không có câu hỏi hợp lệ");
            }

            return questions;

        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc file Word: " + e.getMessage());
        }
    }

    @Override
    public List<QuestionBankRequest> importExcel(MultipartFile file) {

        List<QuestionBankRequest> questions = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            // ===== HEADER =====
            Row header = sheet.getRow(0);

            if (header == null) {
                throw new IllegalArgumentException("Sai định dạng header file mẫu");
            }

            String[] expectedHeaders = {
                    "Câu hỏi",
                    "Đáp án",
                    "Câu trả lời A",
                    "Câu trả lời B",
                    "Câu trả lời C",
                    "Câu trả lời D"
            };

            for (int i = 0; i < 6; i++) {

                Cell cell = header.getCell(i);

                if (cell == null ||
                        !expectedHeaders[i].equalsIgnoreCase(cell.toString().trim())) {
                    throw new IllegalArgumentException("Sai định dạng header file mẫu");
                }
            }

            if (sheet.getLastRowNum() < 1) {
                throw new IllegalArgumentException("File Excel không có dữ liệu");
            }

            // ===== READ DATA =====
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null || isRowEmpty(row)) {
                    throw new IllegalArgumentException("Dòng " + (i + 1) + " bị trống");
                }

                String content = getCell(row, 0);
                String correct = getCell(row, 1).toUpperCase();
                String A = getCell(row, 2);
                String B = getCell(row, 3);
                String C = getCell(row, 4);
                String D = getCell(row, 5);

                if (content.isEmpty())
                    throw new IllegalArgumentException("Dòng " + (i + 1) + " cột câu hỏi thiếu dữ liệu");

                if (correct.isEmpty())
                    throw new IllegalArgumentException("Dòng " + (i + 1) + " cột đáp án thiếu dữ liệu");

                if (A.isEmpty())
                    throw new IllegalArgumentException("Dòng " + (i + 1) + " cột câu trả lời A thiếu dữ liệu");

                if (B.isEmpty())
                    throw new IllegalArgumentException("Dòng " + (i + 1) + " cột câu trả lời B thiếu dữ liệu");

                if (C.isEmpty())
                    throw new IllegalArgumentException("Dòng " + (i + 1) + " cột câu trả lời C thiếu dữ liệu");

                if (D.isEmpty())
                    throw new IllegalArgumentException("Dòng " + (i + 1) + " cột câu trả lời D thiếu dữ liệu");

                if (!correct.matches("[ABCD]")) {
                    throw new IllegalArgumentException(
                            "Dòng " + (i + 1) + " cột đáp án chỉ chấp nhận A B C D");
                }

                List<QuestionOptionRequest> options = List.of(
                        QuestionOptionRequest.builder().optionText(A).isCorrect("A".equals(correct)).build(),
                        QuestionOptionRequest.builder().optionText(B).isCorrect("B".equals(correct)).build(),
                        QuestionOptionRequest.builder().optionText(C).isCorrect("C".equals(correct)).build(),
                        QuestionOptionRequest.builder().optionText(D).isCorrect("D".equals(correct)).build()
                );

                QuestionBankRequest question = QuestionBankRequest.builder()
                        .content(content)
                        .questionType(QuestionType.SINGLE_CHOICE)
                        .options(options)
                        .build();

                questions.add(question);
            }

            return questions;

        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc file Excel");
        }
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

    private boolean isRowEmpty(Row row) {

        for (int i = 0; i < 6; i++) {

            Cell cell = row.getCell(i);

            if (cell != null && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private String getCell(Row row, int index) {

        Cell cell = row.getCell(index);

        if (cell == null) {
            return "";
        }

        return cell.toString().trim();
    }

    private Account getCurrentAccount() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + userId));
    }
}
