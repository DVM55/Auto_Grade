package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.DocumentDetailRequest;
import com.example.Auto_Grade.dto.req.UpdateDocumentDetailRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.DocumentDetailResponse;
import com.example.Auto_Grade.service.DocumentDetailService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/document-detail")
@RequiredArgsConstructor
public class DocumentDetailController {

    private final DocumentDetailService documentDetailService;

    // ================= CREATE =================
    @PostMapping("/{documentId}")
    public ResponseEntity<ApiResponse<List<DocumentDetailResponse>>> createDocumentDetail(
            @PathVariable Long documentId,
            @Valid @RequestBody List<DocumentDetailRequest> requests) {

        List<DocumentDetailResponse> response =
                documentDetailService.createDocumentDetail(documentId, requests);

        return ResponseEntity.ok(
                ApiResponse.<List<DocumentDetailResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Đẩy tài liệu thành công")
                        .data(response)
                        .build()
        );
    }

    // ================= UPDATE =================
    @PutMapping("/{documentDetailId}")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> updateDocumentDetail(
            @PathVariable Long documentDetailId,
            @Valid @RequestBody UpdateDocumentDetailRequest request) {

        DocumentDetailResponse response =
                documentDetailService.updateDocumentDetail(documentDetailId, request);

        return ResponseEntity.ok(
                ApiResponse.<DocumentDetailResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật tài liệu thành công")
                        .data(response)
                        .build()
        );
    }

    // ================= DELETE =================
    @DeleteMapping("/{documentDetailId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentDetail(
            @PathVariable Long documentDetailId) {

        documentDetailService.deleteDocumentDetail(documentDetailId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa file tài liệu thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= GET + PAGINATION =================
    @GetMapping("/document/{documentId}")
    public ResponseEntity<ApiResponse<Page<DocumentDetailResponse>>> getDocumentDetails(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<DocumentDetailResponse> response =
                documentDetailService.getDocumentDetails(documentId, page, size);

        return ResponseEntity.ok(
                ApiResponse.<Page<DocumentDetailResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách file tài liệu thành công")
                        .data(response)
                        .build()
        );
    }
}