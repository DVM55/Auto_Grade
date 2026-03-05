package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.MediaRequest;
import com.example.Auto_Grade.dto.req.UpdateMediaRequest;
import com.example.Auto_Grade.dto.res.MediaResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.Media;
import com.example.Auto_Grade.enums.Role;
import com.example.Auto_Grade.integration.minio.MinioChannel;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.MediaRepository;
import com.example.Auto_Grade.service.MediaService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MediaServiceImpl implements MediaService {
    private final AccountRepository accountRepository;
    private final MediaRepository mediaRepository;
    private final MinioChannel minioChannel;

    private Account getCurrentUser() {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại"));
    }

    @Override
    public void deleteMediaById(Long mediaId) {
        Account currentUser = getCurrentUser();

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media không tồn tại"));

        boolean isOwner = media.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Bạn không có quyền xoá media này");
        }

        mediaRepository.delete(media);
    }

    @Override
    public void createMedia(List<MediaRequest> requests) {
        Account currentUser = getCurrentUser();

        List<Media> medias = requests.stream()
                .map(req -> {

                    String contentType = req.getContentType();

                    if (!isPreviewable(contentType)) {
                        throw new IllegalArgumentException(
                                "Chỉ cho phép upload ảnh, video hoặc audio!"
                        );
                    }

                    return Media.builder()
                            .createdBy(currentUser)
                            .fileName(req.getFileName())
                            .objectKey(req.getObjectKey())
                            .contentType(contentType)
                            .build();
                })
                .toList();

        mediaRepository.saveAll(medias);
    }

    @Override
    public void updateMedia(UpdateMediaRequest request, Long mediaId) {
        Account currentUser = getCurrentUser();

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media không tồn tại"));

        boolean isOwner = media.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa media này");
        }

        media.setFileName(request.getFileName());
        mediaRepository.save(media);
    }

    @Override
    public Page<MediaResponse> getMedias(Long accountId, String fileName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Media> mediaPage = mediaRepository.getMedias(accountId, fileName, pageable);

        return mediaPage.map(this::toResponse);
    }

    public MediaResponse toResponse(Media media) {
        return MediaResponse.builder()
                .id(media.getId())
                .fileUrl(minioChannel.getPresignedUrlSafe(media.getObjectKey(), 3600))
                .fileName(media.getFileName())
                .contentType(media.getContentType())
                .updatedAt(media.getUpdatedAt())
                .build();
    }

    private boolean isPreviewable(String contentType) {

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type không được để trống");
        }

        return (contentType.startsWith("image/")
                || contentType.startsWith("video/")
                || contentType.startsWith("audio/"));
    }
}
