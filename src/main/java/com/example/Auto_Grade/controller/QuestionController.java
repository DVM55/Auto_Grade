package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.QuestionBankRequest;
import com.example.Auto_Grade.dto.req.UpdateQuestionRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.dto.res.QuestionBankResponse;
import com.example.Auto_Grade.enums.QuestionFilterMode;
import com.example.Auto_Grade.enums.QuestionType;
import com.example.Auto_Grade.service.QuestionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    // ───────────── CREATE ────────────
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createQuestionBank(
            @Valid @RequestBody List<QuestionBankRequest> requests) {

        questionService.createQuestionBank(requests);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Tạo câu hỏi thành công")
                .data(null)
                .build());
    }

    // ───────────── UPDATE ─────────────
    @PutMapping("/{questionId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionBankRequest request) {
        questionService.updateQuestion(questionId, request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Cập nhật câu hỏi thành công")
                .data(null)
                .build());
    }

    @PutMapping("/update-by-ids")
    public ResponseEntity<ApiResponse<Void>> updateQuestionByIds(
            @Valid @RequestBody UpdateQuestionRequest request) {

        questionService.updateQuestionByIds(request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Cập nhật thành công")
                .data(null)
                .build());
    }

    // ───────────── DELETE ─────────────
    @DeleteMapping("/{questionId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Xoá câu hỏi thành công")
                .data(null)
                .build());
    }

    @DeleteMapping("")
    public ResponseEntity<ApiResponse<Void>> deleteAllQuestionByCreatorId() {
        questionService.deleteAllQuestionByCreatorId();
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Xoá thành công")
                .data(null)
                .build());
    }

    @GetMapping
    public ResponseEntity<PagingResponse<QuestionBankResponse>> getQuestionBank(
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam QuestionFilterMode questionFilterMode, // bắt buộc
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return ResponseEntity.ok(
                questionService.getQuestion(
                        content,
                        categoryId,
                        groupId,
                        questionType,
                        questionFilterMode,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<ApiResponse<QuestionBankResponse>> getQuestionById(
            @PathVariable Long questionId) {

        QuestionBankResponse result =
                questionService.getQuestionBankById(questionId);

        return ResponseEntity.ok(ApiResponse.<QuestionBankResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy chi tiết câu hỏi thành công")
                .data(result)
                .build());
    }

    @PostMapping(
            value = "/import-word",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<List<QuestionBankRequest>>> importWord(
            @RequestParam("file") MultipartFile file) {

        List<QuestionBankRequest> questions = questionService.importWord(file);

        return ResponseEntity.ok(
                ApiResponse.<List<QuestionBankRequest>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Import câu hỏi thành công")
                        .data(questions)
                        .build()
        );
    }

    @PostMapping(
            value = "/import-excel",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<List<QuestionBankRequest>>> importExcel(
            @RequestParam("file") MultipartFile file) {

        List<QuestionBankRequest> questions = questionService.importExcel(file);

        return ResponseEntity.ok(
                ApiResponse.<List<QuestionBankRequest>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Import câu hỏi thành công")
                        .data(questions)
                        .build()
        );
    }

    @DeleteMapping("/delete-by-ids")
    public ResponseEntity<ApiResponse<Void>> deleteQuestionByIds(
            @RequestBody List<Long> questionIds) {
        questionService.deleteQuestionByIds(questionIds);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Xoá câu hỏi thành công")
                .data(null)
                .build());
    }

}

