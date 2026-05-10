package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.CategoryQuestionRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.CategoryQuestionResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.service.CategoryQuestionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/category-question")
@RequiredArgsConstructor
public class CategoryQuestionController {

    private final CategoryQuestionService categoryQuestionService;

    // ───────────────────── CREATE ─────────────────────
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @RequestBody CategoryQuestionRequest request) {

        categoryQuestionService.createCategoryQuestion(request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Tạo danh mục câu hỏi thành công")
                .data(null)
                .build());
    }

    // ───────────────────── GET MY CATEGORY ─────────────────────
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping
    public ResponseEntity<PagingResponse<CategoryQuestionResponse>> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name
    ) {
        return ResponseEntity.ok(
                categoryQuestionService.getAllCategoryQuestionByCreatorId(page, size, name)
        );
    }

    // ───────────────────── UPDATE ─────────────────────
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryQuestionRequest request) {

        categoryQuestionService.updateCategoryQuestion(id, request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Cập nhật danh mục câu hỏi thành công")
                .data(null)
                .build());
    }

    // ───────────────────── DELETE ─────────────────────
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {

        categoryQuestionService.delete(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Xoá danh mục câu hỏi thành công")
                .data(null)
                .build());
    }
}