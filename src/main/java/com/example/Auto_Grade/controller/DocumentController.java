package com.example.Auto_Grade.controller;


import com.example.Auto_Grade.dto.req.DocumentRequest;
import com.example.Auto_Grade.dto.req.UpdateDocumentRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.DocumentResponse;

import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.service.DocumentExtractionService;
import com.example.Auto_Grade.service.DocumentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentExtractionService documentExtractionService;

    // ================= CREATE =================
    @PostMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<Void>> createDocument(
            @PathVariable Long classId,
            @Valid @RequestBody List<DocumentRequest> requests) {

        documentService.createDocument(requests, classId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Tạo tài liệu thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= UPDATE =================
    @PutMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> updateDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody UpdateDocumentRequest request) {

         documentService.updateDocument(request, documentId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật tài liệu thành công")
                        .data(null)
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
                        .message("Xóa tài liệu thành công")
                        .data(null)
                        .build()
        );
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<PagingResponse<DocumentResponse>> getDocuments(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                documentService.getDocumentsByClassId(classId, page, size)
        );
    }

    @PostMapping(
            value = "/extract-text-file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<byte[]> extractTextFile(@RequestParam("file") MultipartFile file) {
        String extractedText = documentExtractionService.extractText(file);
        byte[] content = extractedText.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(buildExtractedFilename(file.getOriginalFilename()))
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    private String buildExtractedFilename(String originalFilename) {
        String baseName = "extracted";
        if (originalFilename != null && !originalFilename.isBlank()) {
            int dotIndex = originalFilename.lastIndexOf('.');
            baseName = dotIndex > 0 ? originalFilename.substring(0, dotIndex) : originalFilename;
        }

        String safeBaseName = baseName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        if (safeBaseName.isBlank()) {
            safeBaseName = "extracted";
        }

        return safeBaseName + "-extracted.txt";
    }
}
