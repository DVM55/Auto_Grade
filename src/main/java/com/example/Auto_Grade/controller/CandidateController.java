package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.CandidateRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.CandidateResponse;
import com.example.Auto_Grade.service.CandidateService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    // ================= IMPORT EXCEL =================
    @PostMapping(value = "/import/{examId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> importCandidates(
            @PathVariable Long examId,
            @RequestParam("file") MultipartFile file) {

        candidateService.importCandidates(examId, file);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Import danh sách thí sinh thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= UPDATE =================
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponse>> updateCandidate(
            @PathVariable Long id,
            @Valid @RequestBody CandidateRequest request) {

        CandidateResponse response =
                candidateService.updateCandidate(id, request);

        return ResponseEntity.ok(
                ApiResponse.<CandidateResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật thí sinh thành công")
                        .data(response)
                        .build()
        );
    }

    // ================= DELETE CANDIDATE BY ID =================
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCandidate(
            @PathVariable Long id) {

        candidateService.deleteCandidateById(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa thí sinh thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= DELETE ALL CANDIDATES BY EXAM ID =================
    @DeleteMapping("/exam/{examId}")
    public ResponseEntity<ApiResponse<Void>> deleteAllCandidatesByExamId(
            @PathVariable Long examId) {
        candidateService.deleteAllCandidateByExamId(examId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa tất cả thí sinh của kỳ thi thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= GET CANDIDATES BY EXAM ID WITH PAGINATION AND FILTERING =================
    @GetMapping("/exams/{examId}/candidates")
    public ResponseEntity<ApiResponse<Page<CandidateResponse>>> getCandidatesByExamId(

            @PathVariable Long examId,

            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String candidateNumber,
            @RequestParam(required = false) String examRoom,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String className,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<CandidateResponse> result =
                candidateService.getCandidatesByExamId(
                        examId,
                        fullName,
                        candidateNumber,
                        examRoom,
                        note,
                        className,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.<Page<CandidateResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách thí sinh thành công")
                        .data(result)
                        .build()
        );
    }

    @GetMapping("/exams/{examId}/export")
    public ResponseEntity<byte[]> exportCandidates(
            @PathVariable Long examId
    ) {

        byte[] excelData = candidateService.exportCandidatesToExcel(examId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=candidates.xlsx")
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excelData);
    }
}