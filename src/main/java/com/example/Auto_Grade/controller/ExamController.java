package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.ExamRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.ExamResponse;
import com.example.Auto_Grade.service.ExamService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // ================= CREATE =================
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createExam(
            @Valid @RequestBody ExamRequest request) {

        examService.createExam(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Tạo đợt thi thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= UPDATE =================
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamResponse>> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody ExamRequest request) {

        ExamResponse response = examService.updateExam(id, request);

        return ResponseEntity.ok(
                ApiResponse.<ExamResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật đợt thi thành công")
                        .data(response)
                        .build()
        );
    }

    // ================= DELETE =================
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExam(
            @PathVariable Long id) {

        examService.deleteExamById(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa đợt thi thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= GET + SEARCH + PAGINATION =================
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExamResponse>>> getExams(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ExamResponse> response =
                examService.getExams(name, page, size);

        return ResponseEntity.ok(
                ApiResponse.<Page<ExamResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách đợt thi thành công")
                        .data(response)
                        .build()
        );
    }
}