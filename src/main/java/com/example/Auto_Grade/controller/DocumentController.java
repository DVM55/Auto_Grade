package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.DocumentRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.DocumentResponse;
import com.example.Auto_Grade.service.DocumentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // ================= CREATE =================
    @PostMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> createDocument(
            @PathVariable Long classId,
            @Valid @RequestBody DocumentRequest request) {

        DocumentResponse response = documentService.createDocument(request, classId);

        return ResponseEntity.ok(
                ApiResponse.<DocumentResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Tạo thư mục thành công")
                        .data(response)
                        .build()
        );
    }

    // ================= UPDATE =================
    @PutMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentRequest request) {

        DocumentResponse response = documentService.updateDocument(request, documentId);

        return ResponseEntity.ok(
                ApiResponse.<DocumentResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật thư mục thành công")
                        .data(response)
                        .build()
        );
    }

    // ================= DELETE =================
    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long documentId) {

        documentService.delete(documentId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa thư mục thành công")
                        .data(null)
                        .build()
        );
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getDocuments(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<DocumentResponse> response =
                documentService.getDocuments(classId, page, size);

        return ResponseEntity.ok(
                ApiResponse.<Page<DocumentResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách tài liệu thành công")
                        .data(response)
                        .build()
        );
    }
}
