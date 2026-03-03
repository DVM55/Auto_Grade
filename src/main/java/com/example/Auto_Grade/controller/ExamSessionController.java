package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.ExamSessionRequest;
import com.example.Auto_Grade.dto.req.UpdateExamSessionRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.ExamSessionResponse;
import com.example.Auto_Grade.service.ExamSessionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam-session")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    // ================= CREATE =================
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{examId}")
    public ResponseEntity<ApiResponse<Void>> createExamSession(
            @PathVariable Long examId,
            @Valid @RequestBody ExamSessionRequest request) {

        examSessionService.createExamSession(examId, request);

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
    public ResponseEntity<ApiResponse<ExamSessionResponse>> updateExamSession(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamSessionRequest request) {

        ExamSessionResponse response =
                examSessionService.updateExamSession(id, request);

        return ResponseEntity.ok(
                ApiResponse.<ExamSessionResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật đợt thi thành công")
                        .data(response)
                        .build()
        );
    }

    // ================= DELETE =================
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExamSession(
            @PathVariable Long id) {

        examSessionService.deleteExamById(id);

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
    @GetMapping("/{examId}")
    public ResponseEntity<ApiResponse<Page<ExamSessionResponse>>> getExamSessions(
            @PathVariable Long examId,
            @RequestParam(required = false) String sessionName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ExamSessionResponse> response =
                examSessionService.getExamSessionByExamId(
                        examId,
                        sessionName,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.<Page<ExamSessionResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách đợt thi thành công")
                        .data(response)
                        .build()
        );
    }
}