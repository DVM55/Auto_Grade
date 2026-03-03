package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.UpdateAnswerKeyRequest;
import com.example.Auto_Grade.dto.res.AnswerKeyResponse;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.service.AnswerKeyService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/answer-key")
public class AnswerKeyController {

    private final AnswerKeyService answerKeyService;

    // ================= IMPORT ANSWER KEY =================
    @PostMapping(value = "/import/{examSessionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> importAnswerKey(
            @PathVariable Long examSessionId,
            @RequestParam("file") MultipartFile file) {

        answerKeyService.createAnswerKey(examSessionId, file);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Import đáp án thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= GET ALL BY EXAM SESSION =================
    @GetMapping("/exam-session/{examSessionId}")
    public ResponseEntity<ApiResponse<List<AnswerKeyResponse>>> getAllByExamSessionId(
            @PathVariable Long examSessionId) {

        List<AnswerKeyResponse> responses =
                answerKeyService.getAllByExamSessionId(examSessionId);

        return ResponseEntity.ok(
                ApiResponse.<List<AnswerKeyResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách đáp án thành công")
                        .data(responses)
                        .build()
        );
    }

    // ================= DELETE ALL BY EXAM SESSION =================
    @DeleteMapping("/exam-session/{examSessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteAllByExamSessionId(
            @PathVariable Long examSessionId) {

        answerKeyService.deleteAllAnswerKeysByExamSessionId(examSessionId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa toàn bộ đáp án thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= DELETE BY ID =================
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteById(
            @PathVariable Long id) {

        answerKeyService.deleteAnswerKeyById(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa đáp án thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= UPDATE ANSWER KEY =================
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateAnswerKey(
            @PathVariable Long id,
            @RequestBody UpdateAnswerKeyRequest request) {

        answerKeyService.updateAnswerKey(id, request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật đáp án thành công")
                        .data(null)
                        .build()
        );
    }
}