package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.ClassRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.ClassResponse;
import com.example.Auto_Grade.service.ClassService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/class")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    // ================= CREATE =================
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(
            @Valid @RequestBody ClassRequest request) {

        ClassResponse response = classService.createClass(request);

        return ResponseEntity.ok(
                ApiResponse.<ClassResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Tạo lớp thành công")
                        .data(response)
                        .build()
        );
    }

    // ================= UPDATE =================
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody ClassRequest request) {

        ClassResponse response = classService.updateClass(request, id);

        return ResponseEntity.ok(
                ApiResponse.<ClassResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật lớp thành công")
                        .data(response)
                        .build()
        );
    }

    // ================= DELETE =================
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClass(
            @PathVariable Long id) {

        classService.deleteClass(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa lớp thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= GET + SEARCH + PAGINATION =================
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getClasses(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String classCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách lớp thành công")
                        .data(classService.getClasses(title, classCode, page, size))
                        .build()
        );
    }
}
