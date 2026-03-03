package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.UpdateAnswerKeyRequest;
import com.example.Auto_Grade.dto.res.AnswerKeyResponse;
import com.example.Auto_Grade.entity.AnswerKey;
import com.example.Auto_Grade.entity.AnswerKeyDetail;
import com.example.Auto_Grade.entity.ExamSession;
import com.example.Auto_Grade.enums.QuestionPartType;
import com.example.Auto_Grade.exception.ConflictException;
import com.example.Auto_Grade.repository.AnswerKeyRepository;
import com.example.Auto_Grade.repository.ExamSessionRepository;
import com.example.Auto_Grade.service.AnswerKeyService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerKeyServiceImpl implements AnswerKeyService {

    private final AnswerKeyRepository answerKeyRepository;
    private final ExamSessionRepository examSessionRepository;

    // ===================== PUBLIC ENTRY =====================

    @Transactional
    @Override
    public void createAnswerKey(Long examSessionId, MultipartFile file) {
        checkImportPermission(examSessionId);

        validate(file);

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            ExamSession session = examSessionRepository.findById(examSessionId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy exam session với id: " + examSessionId));

            Sheet sheet = workbook.getSheetAt(0);

            Row questionRow = null;
            int questionRowIndex = -1;

            for (int i = 0; i <= sheet.getLastRowNum() - 1; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String val = getCellValue(row.getCell(0));
                if ("mã đề".equalsIgnoreCase(val)) {
                    questionRow = sheet.getRow(i + 1);
                    questionRowIndex = i + 1;
                    break;
                }
            }

            if (questionRow == null)
                throw new IllegalArgumentException("Không tìm thấy header câu hỏi");

            Set<String> existingCodes =
                    answerKeyRepository.findPaperCodesByExamId(
                            session.getExam().getId());

            Map<Integer, ColumnMeta> colMetaMap = buildColumnMetaMap(questionRow);
            int lastCol = questionRow.getLastCellNum();

            for (int r = questionRowIndex + 1; r <= sheet.getLastRowNum(); r++) {

                Row dataRow = sheet.getRow(r);
                if (dataRow == null) continue;

                String paperCode = getCellValue(dataRow.getCell(0));
                if (paperCode == null || paperCode.isBlank()) continue;

                paperCode = paperCode.trim();

                if (existingCodes.contains(paperCode)) {
                    throw new ConflictException(
                            "Mã đề " + paperCode + " đã tồn tại trong hệ thống");
                }

                AnswerKey answerKey = new AnswerKey();
                answerKey.setExamSession(session);
                answerKey.setPaperCode(paperCode);

                List<AnswerKeyDetail> details = new ArrayList<>();

                for (int c = 1; c < lastCol; c++) {

                    ColumnMeta meta = colMetaMap.get(c);
                    if (meta == null) continue;

                    String answerValue = getCellValue(dataRow.getCell(c));
                    if (answerValue == null || answerValue.isBlank()) continue;

                    AnswerKeyDetail detail = buildDetail(meta, answerValue.trim());
                    detail.setAnswerKey(answerKey);
                    details.add(detail);
                }

                answerKey.setDetails(details);
                answerKeyRepository.save(answerKey);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteAllAnswerKeysByExamSessionId(Long examSessionId) {
        checkImportPermission(examSessionId);
        ExamSession session = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy exam session với id: " + examSessionId));
        answerKeyRepository.deleteByExamSession_Id(session.getExam().getId());
    }

    @Override
    @Transactional
    public void deleteAnswerKeyById(Long id) {
        AnswerKey answerKey = answerKeyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy answer key với id: " + id));
        checkImportPermission(answerKey.getExamSession().getId());
        answerKeyRepository.delete(answerKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnswerKeyResponse> getAllByExamSessionId(Long examSessionId) {

        ExamSession session = examSessionRepository.findById(examSessionId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy đợt thi"));

        checkImportPermission(examSessionId);

        List<AnswerKey> answerKeys =
                answerKeyRepository.findAllWithDetailsByExamSession_Id(session.getId());

        return answerKeys.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void updateAnswerKey(
            Long answerKeyId,
            UpdateAnswerKeyRequest request
    ) {

        AnswerKey answerKey = answerKeyRepository.findById(answerKeyId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy answer key với id: " + answerKeyId
                        )
                );

        checkImportPermission(answerKey.getExamSession().getId());

        // ================= VALIDATE PAPER CODE =================
        String paperCode = request.getPaperCode() == null
                ? null
                : request.getPaperCode().trim();

        if (paperCode == null || !paperCode.matches("\\d{4}")) {
            throw new IllegalArgumentException("Mã đề phải gồm 4 chữ số");
        }

        boolean isDuplicate =
                answerKeyRepository.existsByExamSession_IdAndPaperCodeAndIdNot(
                        answerKey.getExamSession().getId(),
                        paperCode,
                        answerKeyId
                );

        if (isDuplicate) {
            throw new ConflictException(
                    "Mã đề " + paperCode + " đã tồn tại trong đợt thi này"
            );
        }

        answerKey.setPaperCode(paperCode);

        Map<String, String> part1 = normalizeMap(request.getPart1());
        Map<String, String> part2 = normalizeMap(request.getPart2());
        Map<String, String> part3 = normalizeMap(request.getPart3());

        validatePart1(part1);
        validatePart2(part2);
        validatePart3(part3);

        answerKey.getDetails().clear();
        answerKeyRepository.flush();

// PART I
        part1.forEach((k, v) -> {
            AnswerKeyDetail d = new AnswerKeyDetail();
            d.setAnswerKey(answerKey);
            d.setPartType(QuestionPartType.PART1);
            d.setQuestionNumber(Integer.parseInt(k.trim()));
            d.setCorrectValue(v.trim().toUpperCase());
            answerKey.getDetails().add(d);
        });

// PART II
        part2.forEach((k, v) -> {

            String key = k.trim().toLowerCase();
            String value = v.trim();

            int q = Integer.parseInt(key.replaceAll("[^0-9]", ""));
            String sub = key.replaceAll("[0-9]", "");

            AnswerKeyDetail d = new AnswerKeyDetail();
            d.setAnswerKey(answerKey);
            d.setPartType(QuestionPartType.PART2);
            d.setQuestionNumber(q);
            d.setSubQuestion(sub);
            d.setCorrectValue(
                    value.equalsIgnoreCase("ĐÚNG") ? "TRUE" : "FALSE"
            );

            answerKey.getDetails().add(d);
        });

// PART III
        part3.forEach((k, v) -> {

            AnswerKeyDetail d = new AnswerKeyDetail();
            d.setAnswerKey(answerKey);
            d.setPartType(QuestionPartType.PART3);
            d.setQuestionNumber(Integer.parseInt(k.trim()));
            d.setCorrectValue(v.trim().replace(".", ","));
            answerKey.getDetails().add(d);
        });
    }

    // ===================== VALIDATE FILE =====================

    private void validate(MultipartFile file) {

        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File không được để trống");

        String filename = file.getOriginalFilename();
        if (filename == null ||
                (!filename.endsWith(".xls") && !filename.endsWith(".xlsx")))
            throw new IllegalArgumentException("File phải có định dạng .xls hoặc .xlsx");

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            findHeadersAndValidate(sheet);

        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc file: " + e.getMessage());
        }
    }

    // ===================== FIND HEADER =====================

    private void findHeadersAndValidate(Sheet sheet) {

        Row questionRow = null;
        int questionRowIndex = -1;

        for (int i = 0; i <= sheet.getLastRowNum() - 1; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String val = getCellValue(row.getCell(0));
            if ("mã đề".equalsIgnoreCase(val)) {
                questionRow = sheet.getRow(i + 1);
                questionRowIndex = i + 1;
                break;
            }
        }

        if (questionRow == null)
            throw new IllegalArgumentException("File excel phải đúng định dạng tiêu đề so với file mẫu!");

        Map<Integer, ColumnMeta> colMetaMap = buildColumnMetaMap(questionRow);

        Set<String> seenPaperCodes = new HashSet<>();
        int lastCol = questionRow.getLastCellNum();
        int dataCount = 0;

        for (int r = questionRowIndex + 1; r <= sheet.getLastRowNum(); r++) {

            Row dataRow = sheet.getRow(r);
            if (dataRow == null) continue;

            String paperCode = getCellValue(dataRow.getCell(0));
            if (paperCode == null || paperCode.isBlank()) continue;

            dataCount++;
            paperCode = paperCode.trim();

            if (!paperCode.matches("\\d{4}"))
                throw new IllegalArgumentException("Mã đề phải gồm 4 chữ số");

            if (!seenPaperCodes.add(paperCode))
                throw new IllegalArgumentException("Trùng mã đề: " + paperCode);

            for (int c = 1; c < lastCol; c++) {

                ColumnMeta meta = colMetaMap.get(c);
                if (meta == null) continue;

                String answer = getCellValue(dataRow.getCell(c));
                if (answer == null || answer.isBlank())
                    throw new IllegalArgumentException(
                            "Thiếu đáp án tại mã đề " + paperCode +
                                    ", cột " + meta.header);

                validateAnswer(answer.trim(), meta, paperCode);
            }
        }

        if (dataCount == 0)
            throw new IllegalArgumentException("File không có dữ liệu đáp án");
    }

    // ===================== BUILD COLUMN META =====================

    private Map<Integer, ColumnMeta> buildColumnMetaMap(Row questionRow) {

        Map<Integer, ColumnMeta> map = new LinkedHashMap<>();

        Set<Integer> part1 = new HashSet<>();
        Set<String> part2 = new HashSet<>();
        Set<Integer> part3 = new HashSet<>();

        int lastCol = questionRow.getLastCellNum();
        QuestionPartType currentPart = QuestionPartType.PART1;

        for (int c = 1; c < lastCol; c++) {

            String header = getCellValue(questionRow.getCell(c));
            if (header == null || header.isBlank()) continue;
            header = header.trim();

            ColumnMeta meta = new ColumnMeta();
            meta.header = header;
            meta.colIndex = c;

            if (header.matches("\\d+")) {

                if (currentPart != QuestionPartType.PART1)
                    throw new IllegalArgumentException("Phần I phải nằm trước các phần khác");

                int number = Integer.parseInt(header);
                if (number < 1 || number > 40)
                    throw new IllegalArgumentException("Phần I chỉ từ 1-40");

                if (!part1.add(number))
                    throw new IllegalArgumentException("Trùng câu " + header);

                meta.partType = QuestionPartType.PART1;
            }

            else if (header.matches("\\d+[a-dA-D]")) {

                currentPart = QuestionPartType.PART2;

                int q = Integer.parseInt(header.replaceAll("[^0-9]", ""));
                String sub = header.replaceAll("[0-9]", "").toLowerCase();

                if (q < 1 || q > 8)
                    throw new IllegalArgumentException("Phần II chỉ từ 1-8");

                String key = q + sub;
                if (!part2.add(key))
                    throw new IllegalArgumentException("Trùng cột " + header);

                meta.partType = QuestionPartType.PART2;
            }

            else if (header.toUpperCase().matches("CÂU\\s*\\d+")) {

                currentPart = QuestionPartType.PART3;

                int number = Integer.parseInt(header.replaceAll("[^0-9]", ""));
                if (number < 1 || number > 6)
                    throw new IllegalArgumentException("Phần III chỉ gồm câu từ 1-6");

                if (!part3.add(number))
                    throw new IllegalArgumentException("Trùng " + header);

                meta.partType = QuestionPartType.PART3;
            }

            else {
                String excelCol = CellReference.convertNumToColString(c);
                int excelRow = questionRow.getRowNum() + 1; // vì Excel bắt đầu từ 1

                throw new IllegalArgumentException(
                        "Cột " + excelCol + " dòng "+ excelRow + " không hợp lệ: ");
            }

            map.put(c, meta);
        }

        if (part1.size() != 40)
            throw new IllegalArgumentException("Phần I phải đủ 40 cột");

        if (part2.size() != 32)
            throw new IllegalArgumentException("Phần II phải đủ 32 cột");

        if (part3.size() != 6)
            throw new IllegalArgumentException("Phần III phải đủ 6 cột");

        return map;
    }

    // ===================== VALIDATE ANSWER =====================

    private void validateAnswer(String answer, ColumnMeta meta, String paperCode) {

        switch (meta.partType) {

            case PART1 -> {
                if (!answer.matches("[A-Da-d]"))
                    throw new IllegalArgumentException(
                            "Mã đề " + paperCode +
                                    " – Cột " + meta.header +
                                    " chỉ chấp nhận A,B,C,D");
            }

            case PART2 -> {
                if (!answer.equalsIgnoreCase("ĐÚNG") &&
                        !answer.equalsIgnoreCase("SAI"))
                    throw new IllegalArgumentException(
                            "Mã đề " + paperCode +
                                    " – Cột " + meta.header +
                                    " chỉ chấp nhận ĐÚNG/SAI");
            }

            case PART3 -> {
                if (!answer.matches("-?[0-9]+(,[0-9]+)?"))
                    throw new IllegalArgumentException(
                            "Mã đề " + paperCode +
                                    " – Cột " + meta.header +
                                    " chỉ được chứa số, dấu '-' và dấu ','");

                if (answer.length() > 6)
                    throw new IllegalArgumentException(
                            "Mã đề " + paperCode +
                                    " – Cột " + meta.header +
                                    " vượt quá 6 ký tự");
            }
        }
    }

    // ===================== BUILD DETAIL =====================

    private AnswerKeyDetail buildDetail(ColumnMeta meta, String answer) {

        AnswerKeyDetail detail = new AnswerKeyDetail();
        detail.setPartType(meta.partType);

        switch (meta.partType) {

            case PART1 -> {
                detail.setQuestionNumber(Integer.parseInt(meta.header));
                detail.setCorrectValue(answer.trim().toUpperCase());
            }

            case PART2 -> {
                int q = Integer.parseInt(meta.header.replaceAll("[^0-9]", ""));
                String sub = meta.header.replaceAll("[0-9]", "").toLowerCase();
                detail.setQuestionNumber(q);
                detail.setSubQuestion(sub);
                detail.setCorrectValue(
                        answer.trim().equalsIgnoreCase("ĐÚNG") ? "TRUE" : "FALSE");
            }

            case PART3 -> {
                int q = Integer.parseInt(meta.header.replaceAll("[^0-9]", ""));
                detail.setQuestionNumber(q);
                detail.setCorrectValue(answer.trim().replace(".", ","));
            }
        }

        return detail;
    }

    // ===================== GET CELL =====================

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {

            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val))
                    return String.valueOf((long) val);
                return String.valueOf(val).replace(".", ",");

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                FormulaEvaluator evaluator = cell.getSheet()
                        .getWorkbook()
                        .getCreationHelper()
                        .createFormulaEvaluator();
                CellValue evaluated = evaluator.evaluate(cell);
                if (evaluated == null) return null;

                if (evaluated.getCellType() == CellType.NUMERIC)
                    return String.valueOf(evaluated.getNumberValue())
                            .replace(".", ",");

                if (evaluated.getCellType() == CellType.STRING)
                    return evaluated.getStringValue();

                return null;

            default:
                return null;
        }
    }

    // ===================== META =====================

    private static class ColumnMeta {
        String header;
        int colIndex;
        QuestionPartType partType;
    }

    private void checkImportPermission(Long examSessionId) {

        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        boolean hasPermission =
                examSessionRepository.hasImportPermission(
                        examSessionId,
                        userId
                );

        if (!hasPermission) {
            throw new AccessDeniedException(
                    "Bạn không có quyền xem/thêm/sửa/xóa đáp án đợt thi này"
            );
        }
    }

    private AnswerKeyResponse mapToResponse(AnswerKey key) {

        Map<String, String> part1 = new TreeMap<>();
        Map<String, String> part2 = new TreeMap<>();
        Map<String, String> part3 = new TreeMap<>();

        for (AnswerKeyDetail d : key.getDetails()) {

            String questionKey = String.valueOf(d.getQuestionNumber());

            switch (d.getPartType()) {

                case PART1 -> part1.put(
                        questionKey,
                        d.getCorrectValue()
                );

                case PART2 -> {
                    String combinedKey =
                            d.getQuestionNumber() + d.getSubQuestion();

                    part2.put(
                            combinedKey,
                            convertTrueFalse(d.getCorrectValue())
                    );
                }

                case PART3 -> part3.put(
                        questionKey,
                        d.getCorrectValue()
                );
            }
        }

        return AnswerKeyResponse.builder()
                .id(key.getId())
                .paperCode(key.getPaperCode())
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .build();
    }

    private String convertTrueFalse(String value) {
        if ("TRUE".equalsIgnoreCase(value)) return "Đúng";
        if ("FALSE".equalsIgnoreCase(value)) return "Sai";
        return value;
    }

    private void validatePart1(Map<String, String> part1) {

        if (part1 == null || part1.size() != 40)
            throw new IllegalArgumentException("Phần I phải đủ 40 câu");

        Set<Integer> seen = new HashSet<>();

        part1.forEach((k, v) -> {

            String key = k.trim();
            String value = v.trim();

            if (!key.matches("\\d+"))
                throw new IllegalArgumentException("Câu Part I không hợp lệ: " + key);

            int q = Integer.parseInt(key);

            if (q < 1 || q > 40)
                throw new IllegalArgumentException("Phần I chỉ từ 1-40");

            if (!seen.add(q))
                throw new IllegalArgumentException("Trùng câu Part I: " + key);

            if (!value.matches("[A-Da-d]"))
                throw new IllegalArgumentException(
                        "Part I câu " + key + " chỉ chấp nhận A,B,C,D");
        });
    }

    private void validatePart2(Map<String, String> part2) {

        if (part2 == null || part2.size() != 32)
            throw new IllegalArgumentException(
                    "Phần II phải đủ 32 đáp án (8 câu × 4 ý)");

        Set<String> expectedKeys = new HashSet<>();

        for (int i = 1; i <= 8; i++) {
            expectedKeys.add(i + "a");
            expectedKeys.add(i + "b");
            expectedKeys.add(i + "c");
            expectedKeys.add(i + "d");
        }

        for (String rawKey : part2.keySet()) {

            String key = rawKey.trim().toLowerCase();

            if (!expectedKeys.contains(key))
                throw new IllegalArgumentException(
                        "Thiếu hoặc sai cấu trúc định dạng phần 2: " + key);

            String value = part2.get(rawKey).trim();

            if (!value.equalsIgnoreCase("ĐÚNG") &&
                    !value.equalsIgnoreCase("SAI"))
                throw new IllegalArgumentException(
                        "Part II " + key + " chỉ chấp nhận ĐÚNG/SAI");
        }
    }

    private void validatePart3(Map<String, String> part3) {

        if (part3 == null || part3.size() != 6)
            throw new IllegalArgumentException("Phần III phải đủ 6 câu");

        Set<Integer> seen = new HashSet<>();

        part3.forEach((k, v) -> {

            String key = k.trim();
            String value = v.trim();

            if (!key.matches("\\d+"))
                throw new IllegalArgumentException("Câu Part III không hợp lệ");

            int q = Integer.parseInt(key);

            if (q < 1 || q > 6)
                throw new IllegalArgumentException("Phần III chỉ từ 1-6");

            if (!seen.add(q))
                throw new IllegalArgumentException("Trùng câu Part III: " + key);

            if (!value.matches("-?[0-9]+(,[0-9]+)?"))
                throw new IllegalArgumentException(
                        "Part III câu " + key +
                                " chỉ chứa số, '-' và ','");

            if (value.length() > 6)
                throw new IllegalArgumentException(
                        "Part III câu " + key + " vượt quá 6 ký tự");
        });
    }

    private Map<String, String> normalizeMap(Map<String, String> input) {

        if (input == null) return null;

        Map<String, String> normalized = new HashMap<>();

        input.forEach((k, v) -> {

            String key = k == null ? null : k.trim().toLowerCase();
            String value = v == null ? null : v.trim();

            normalized.put(key, value);
        });

        return normalized;
    }


}