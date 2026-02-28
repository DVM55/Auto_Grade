package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.CandidateRequest;
import com.example.Auto_Grade.dto.res.CandidateResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.Candidate;
import com.example.Auto_Grade.entity.Exam;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.CandidateRepository;
import com.example.Auto_Grade.repository.ExamRepository;
import com.example.Auto_Grade.service.CandidateService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final AccountRepository accountRepository;
    private final ExamRepository examRepository;

    // ================= IMPORT EXCEL =================
    @Override
    @Transactional
    public void importCandidates(Long examId, MultipartFile file) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy đợt thi với id: " + examId)
                );

        Account currentAccount = getCurrentAccount();

        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException("Bạn không có quyền import thí sinh");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            validateHeader(sheet.getRow(0));

            // ===== LẤY SBD ĐÃ TỒN TẠI TRONG DB (1 query duy nhất) =====
            Set<String> existingNumbers = candidateRepository.findAllByExam_Id(examId)
                    .stream()
                    .map(Candidate::getCandidateNumber)
                    .collect(Collectors.toSet());

            Set<String> fileNumbers = new HashSet<>();
            List<Candidate> candidates = new ArrayList<>();

            for (Row row : sheet) {

                if (row.getRowNum() == 0) continue; // bỏ header
                if (isRowEmpty(row)) continue;

                int rowIndex = row.getRowNum() + 1;

                String fullName = getCellValue(row.getCell(0));
                String candidateNumber = getCellValue(row.getCell(1));
                String examRoom = getCellValue(row.getCell(2));
                String note = getCellValue(row.getCell(3));
                String className = getCellValue(row.getCell(4));
                LocalDate dob = getDateValue(row.getCell(5), rowIndex);
                String gender = getCellValue(row.getCell(6));

                // ===== VALIDATE =====
                if (isBlank(fullName))
                    throw new IllegalArgumentException("Dòng " + rowIndex + " thiếu họ và tên");

                if (isBlank(candidateNumber))
                    throw new IllegalArgumentException("Dòng " + rowIndex + " thiếu số báo danh");

                if (isBlank(examRoom))
                    throw new IllegalArgumentException("Dòng " + rowIndex + " thiếu phòng thi");

                if (isBlank(className))
                    throw new IllegalArgumentException("Dòng " + rowIndex + " thiếu lớp");

                // ===== CHECK TRÙNG TRONG FILE =====
                if (!fileNumbers.add(candidateNumber))
                    throw new IllegalArgumentException(
                            "Dòng " + rowIndex + " trùng số báo danh trong file: " + candidateNumber);

                // ===== CHECK TRÙNG TRONG DB =====
                if (existingNumbers.contains(candidateNumber))
                    throw new IllegalArgumentException(
                            "Dòng " + rowIndex + " bị trùng số báo danh: " + candidateNumber +" trong kỳ thi này");

                Candidate candidate = Candidate.builder()
                        .fullName(fullName)
                        .candidateNumber(candidateNumber)
                        .examRoom(examRoom)
                        .note(note)
                        .className(className)
                        .dateOfBirth(dob)
                        .gender(gender)
                        .exam(exam)
                        .build();

                candidates.add(candidate);
            }

            candidateRepository.saveAll(candidates);

        } catch (IOException e) {
            throw new RuntimeException("Không đọc được file Excel", e);
        }
    }

    private void validateHeader(Row header) {

        if (header == null)
            throw new IllegalArgumentException("File Excel không có header");

        String[] expected = {
                "Họ và tên",
                "Số báo danh",
                "Phòng thi",
                "Ghi chú",
                "Lớp",
                "Ngày sinh",
                "Giới tính"
        };

        for (int i = 0; i < expected.length; i++) {
            String actual = getCellValue(header.getCell(i));
            if (!expected[i].equalsIgnoreCase(actual)) {
                throw new IllegalArgumentException("Header không đúng định dạng mẫu");
            }
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();

            case NUMERIC -> {
                if (!DateUtil.isCellDateFormatted(cell)) {
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
                yield null;
            }

            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());

            case FORMULA -> {
                FormulaEvaluator evaluator =
                        cell.getSheet().getWorkbook()
                                .getCreationHelper()
                                .createFormulaEvaluator();

                CellValue value = evaluator.evaluate(cell);

                yield switch (value.getCellType()) {
                    case STRING -> value.getStringValue().trim();
                    case NUMERIC -> String.valueOf((long) value.getNumberValue());
                    case BOOLEAN -> String.valueOf(value.getBooleanValue());
                    default -> null;
                };
            }

            default -> null;
        };
    }

    private LocalDate getDateValue(Cell cell, int rowIndex) {

        if (cell == null) return null;

        try {

            return switch (cell.getCellType()) {

                case BLANK -> null;

                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        yield cell.getLocalDateTimeCellValue().toLocalDate();
                    }
                    throw new IllegalArgumentException(
                            "Dòng " + rowIndex +
                                    " ngày sinh phải là định dạng Date trong Excel"
                    );
                }

                case STRING -> {
                    String value = cell.getStringCellValue().trim();

                    if (value.isEmpty()) {
                        yield null;
                    }

                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("d/M/yyyy");

                    yield LocalDate.parse(value, formatter);
                }

                default -> throw new IllegalArgumentException(
                        "Dòng " + rowIndex +
                                " giá trị ngày sinh không hợp lệ"
                );
            };

        } catch (IllegalArgumentException e) {
            throw e; // giữ nguyên message custom
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Dòng " + rowIndex +
                            " sai định dạng ngày sinh. Yêu cầu dạng: dd/MM/yyyy (vd: 31/12/2000)"
            );
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    // ================= UPDATE =================
    @Override
    @Transactional
    public CandidateResponse updateCandidate(Long id, CandidateRequest request) {

        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy thí sinh với id: " + id)
                );

        Account currentAccount = getCurrentAccount();

        if (!candidate.getExam().getCreator().getId()
                .equals(currentAccount.getId())) {

            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa thí sinh này");
        }

        candidate.setFullName(request.getFullName());
        candidate.setExamRoom(request.getExamRoom());
        candidate.setClassName(request.getClassName());
        candidate.setDateOfBirth(request.getDateOfBirth());
        candidate.setNote(request.getNote());
        candidate.setGender(request.getGender());

        candidateRepository.save(candidate);

        return mapToResponse(candidate);
    }

    // ================= DELETE =================
    @Override
    @Transactional
    public void deleteCandidateById(Long id) {

        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy thí sinh với id: " + id)
                );

        Account currentAccount = getCurrentAccount();

        if (!candidate.getExam().getCreator().getId()
                .equals(currentAccount.getId())) {

            throw new AccessDeniedException("Bạn không có quyền xoá thí sinh này");
        }

        candidateRepository.delete(candidate);
    }

    @Override
    @Transactional
    public void deleteAllCandidateByExamId(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy đợt thi với id: " + id)
                );

        Account currentAccount = getCurrentAccount();

        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xóa thí sinh");
        }
        candidateRepository.deleteAllByExam_Id(id);
    }

    @Override
    public Page<CandidateResponse> getCandidatesByExamId(
            Long id,
            String fullName,
            String candidateNumber,
            String examRoom,
            String note,
            String className,
            int page,
            int size
    ) {

        Exam exam = examRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy đợt thi với id: " + id)
                );

        Account currentAccount = getCurrentAccount();

        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem danh sách thí sinh");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Candidate> candidatePage =
                candidateRepository.findByExamIdWithFilters(
                        id,
                        fullName,
                        candidateNumber,
                        examRoom,
                        note,
                        className,
                        pageable
                );

        return candidatePage.map(this::mapToResponse);
    }

    @Override
    public byte[] exportCandidatesToExcel(Long examId) {

        // ===== 1. Tìm exam =====
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy kỳ thi với id: " + examId
                        )
                );

        // ===== 2. Lấy user hiện tại =====
        Account currentAccount = getCurrentAccount();

        // ===== 3. Check quyền =====
        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException(
                    "Bạn không có quyền xuất danh sách thí sinh"
            );
        }

        List<Candidate> candidates =
                candidateRepository.findAllByExam_IdOrderByIdAsc(examId);

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Candidates");

            // ================= HEADER STYLE =================
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // ================= DATE STYLE =================
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(
                    createHelper.createDataFormat().getFormat("dd/MM/yyyy")
            );

            // ================= HEADER =================
            String[] columns = {
                    "Họ và tên",
                    "Số báo danh",
                    "Phòng thi",
                    "Ghi chú",
                    "Lớp",
                    "Ngày sinh",
                    "Giới tính"
            };

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // ================= DATA =================
            int rowIdx = 1;

            for (Candidate c : candidates) {

                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(c.getFullName());
                row.createCell(1).setCellValue(c.getCandidateNumber());
                row.createCell(2).setCellValue(c.getExamRoom());
                row.createCell(3).setCellValue(
                        c.getNote() == null ? "" : c.getNote()
                );
                row.createCell(4).setCellValue(c.getClassName());

                // ===== Ngày sinh đúng chuẩn Excel Date =====
                if (c.getDateOfBirth() != null) {
                    Cell dateCell = row.createCell(5);
                    dateCell.setCellValue(
                            java.sql.Date.valueOf(c.getDateOfBirth())
                    );
                    dateCell.setCellStyle(dateStyle);
                }

                row.createCell(6).setCellValue(
                        c.getGender() == null ? "" : c.getGender()
                );
            }

            // Auto size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Freeze header
            sheet.createFreezePane(0, 1);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);

            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất file Excel", e);
        }
    }

    // ================= MAPPER =================
    private CandidateResponse mapToResponse(Candidate candidate) {
        return CandidateResponse.builder()
                .id(candidate.getId())
                .fullName(candidate.getFullName())
                .candidateNumber(candidate.getCandidateNumber())
                .examRoom(candidate.getExamRoom())
                .className(candidate.getClassName())
                .dateOfBirth(candidate.getDateOfBirth())
                .note(candidate.getNote())
                .gender(candidate.getGender())
                .build();
    }

    // ================= CURRENT USER =================
    private Account getCurrentAccount() {
        Long userId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return accountRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy tài khoản với id: " + userId));
    }
}