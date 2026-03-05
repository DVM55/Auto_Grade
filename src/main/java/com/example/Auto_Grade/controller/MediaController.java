package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.MediaRequest;
import com.example.Auto_Grade.dto.req.UpdateMediaRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.MediaResponse;
import com.example.Auto_Grade.service.MediaService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/medias")
public class MediaController {

    private final MediaService mediaService;

    // CREATE
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createMedia(
            @Valid @RequestBody List<MediaRequest> requests) {

        mediaService.createMedia(requests);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Upload media thành công")
                        .build()
        );
    }

    // UPDATE
    @PutMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> updateMedia(
            @PathVariable Long mediaId,
            @Valid @RequestBody UpdateMediaRequest request) {

        mediaService.updateMedia(request, mediaId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật media thành công")
                        .build()
        );
    }

    // DELETE
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable Long mediaId) {

        mediaService.deleteMediaById(mediaId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa media thành công")
                        .build()
        );
    }

    // GET LIST
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MediaResponse>>> getMedias(
            @RequestParam Long accountId,
            @RequestParam(required = false) String fileName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<MediaResponse> response = mediaService.getMedias(accountId, fileName, page, size);

        return ResponseEntity.ok(
                ApiResponse.<Page<MediaResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách media thành công")
                        .data(response)
                        .build()
        );
    }
}