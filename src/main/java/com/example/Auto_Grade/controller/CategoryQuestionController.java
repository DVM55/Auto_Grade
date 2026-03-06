package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.CategoryQuestionRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.CategoryQuestionResponse;
import com.example.Auto_Grade.service.CategoryQuestionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/category-question")
@RequiredArgsConstructor
public class CategoryQuestionController {

    private final CategoryQuestionService categoryQuestionService;

    // ───────────────────── CREATE ─────────────────────
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
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryQuestionResponse>>> getMyCategories() {

        return ResponseEntity.ok(ApiResponse.<List<CategoryQuestionResponse>>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy danh sách danh mục thành công")
                .data(categoryQuestionService.getAllCategoryQuestionByCreatorId())
                .build());
    }

    // ───────────────────── UPDATE ─────────────────────
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