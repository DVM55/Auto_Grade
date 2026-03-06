package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.GroupQuestionRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.GroupQuestionResponse;
import com.example.Auto_Grade.service.GroupQuestionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group-question")
@RequiredArgsConstructor
public class GroupQuestionController {

    private final GroupQuestionService groupQuestionService;

    // ───────────────────── CREATE ─────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @RequestBody GroupQuestionRequest request) {

        groupQuestionService.createGroupQuestion(request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Tạo nhóm câu hỏi thành công")
                .data(null)
                .build());
    }

    // ───────────────────── GET MY GROUP ─────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupQuestionResponse>>> getMyGroups() {

        return ResponseEntity.ok(ApiResponse.<List<GroupQuestionResponse>>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy danh sách nhóm câu hỏi thành công")
                .data(groupQuestionService.getAllGroupQuestionByCreatorId())
                .build());
    }

    // ───────────────────── UPDATE ─────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id,
            @Valid @RequestBody GroupQuestionRequest request) {

        groupQuestionService.updateGroupQuestion(id, request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Cập nhật nhóm câu hỏi thành công")
                .data(null)
                .build());
    }

    // ───────────────────── DELETE ─────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {

        groupQuestionService.delete(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Xoá nhóm câu hỏi thành công")
                .data(null)
                .build());
    }
}