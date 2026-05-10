package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.MediaRequest;
import com.example.Auto_Grade.dto.req.UpdateMediaRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.MediaResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.enums.MediaType;
import com.example.Auto_Grade.service.MediaService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/medias")
public class MediaController {

    private final MediaService mediaService;

    // CREATE
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
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

    @DeleteMapping("")
    public ResponseEntity<ApiResponse<Void>> deleteAllMediaByCreator() {
        mediaService.deleteAllMediaByCreator();
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa media thành công")
                        .build()
        );
    }

    // GET LIST
    @GetMapping
    public ResponseEntity<PagingResponse<MediaResponse>> getMedias(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                mediaService.getMediasByCreator(fileName, mediaType, page, size)
        );
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> deleteMediasByIds(
            @RequestBody List<Long> mediaIds) {

        mediaService.deleteMediasByIds(mediaIds);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa media thành công")
                        .build()
        );

    }

    @DeleteMapping("/type/{mediaType}")
    public ResponseEntity<ApiResponse<Void>> deleteAllByMediaType(
            @PathVariable String mediaType) {

        MediaType type = MediaType.valueOf(mediaType.toUpperCase());

        mediaService.deleteAllByMediaType(type);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa thành công")
                        .build()
        );
    }
}