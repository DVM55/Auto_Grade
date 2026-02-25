package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.DocumentDetailRequest;
import com.example.Auto_Grade.dto.req.UpdateDocumentDetailRequest;
import com.example.Auto_Grade.dto.res.DocumentDetailResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.Document;
import com.example.Auto_Grade.entity.DocumentDetail;
import com.example.Auto_Grade.enums.MemberStatus;
import com.example.Auto_Grade.integration.minio.MinioChannel;
import com.example.Auto_Grade.repository.*;
import com.example.Auto_Grade.service.DocumentDetailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentDetailServiceImpl implements DocumentDetailService {

    private final DocumentDetailRepository documentDetailRepository;
    private final DocumentRepository documentRepository;
    private final MinioChannel minioChannel;
    private final AccountRepository accountRepository;
    private final ClassRepository classRepository;

    @Override
    @Transactional
    public List<DocumentDetailResponse> createDocumentDetail(
            Long documentId,
            List<DocumentDetailRequest> requests
    ) {

        Account currentUser = getCurrentUser();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy thư mục với id: " + documentId)
                );

        // 🔐 CHECK QUYỀN
        if (!document.getClassEntity().getCreator().getId()
                .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "Chỉ người tạo lớp mới được upload tài liệu"
            );
        }

        List<DocumentDetail> details = requests.stream()
                .map(req -> DocumentDetail.builder()
                        .document(document)
                        .fileName(req.getFileName())
                        .objectKey(req.getObjectKey())
                        .contentType(req.getContentType())
                        .fileSize(req.getFileSize())
                        .build()
                )
                .toList();

        List<DocumentDetail> saved = documentDetailRepository.saveAll(details);

        return saved.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DocumentDetailResponse updateDocumentDetail(Long documentDetailId, UpdateDocumentDetailRequest requests) {
        Account currentUser = getCurrentUser();

        DocumentDetail documentDetail = documentDetailRepository.findById(documentDetailId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy tài liệu với id: " + documentDetailId)
                );

        // 🔐 CHECK QUYỀN
        if (!documentDetail.getDocument().getClassEntity().getCreator().getId()
                .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "Chỉ người tạo lớp mới được chỉnh tài liệu này"
            );
        }

        documentDetail.setFileName(requests.getFileName());
        documentDetailRepository.save(documentDetail);

        return toResponse(documentDetail);
    }

    @Override
    public void deleteDocumentDetail(Long documentDetailId) {
        Account currentUser = getCurrentUser();

        DocumentDetail documentDetail = documentDetailRepository.findById(documentDetailId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy tài liệu với id: " + documentDetailId)
                );

        // 🔐 CHECK QUYỀN
        if (!documentDetail.getDocument().getClassEntity().getCreator().getId()
                .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "Chỉ người tạo lớp mới được xóa tài liệu"
            );
        }

        documentDetailRepository.delete(documentDetail);
    }

    @Override
    public Page<DocumentDetailResponse> getDocumentDetails(
            Long documentId,
            int page,
            int size
    ) {

        Account currentUser = getCurrentUser();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy thư mục với id: " + documentId)
                );

        Long classId = document.getClassEntity().getId();

        boolean hasAccess = classRepository.hasAccessToClass(classId, currentUser.getId());

        if (!hasAccess) {
            throw new AccessDeniedException("Bạn không có quyền xem tài liệu của lớp này");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<DocumentDetail> detailPage =
                documentDetailRepository.findByDocument_Id(documentId, pageable);

        return detailPage.map(this::toResponse);
    }

    public DocumentDetailResponse toResponse(DocumentDetail documentDetail) {
        return DocumentDetailResponse.builder()
                .id(documentDetail.getId())
                .fileName(documentDetail.getFileName())
                .contentType(documentDetail.getContentType())
                .fileUrl(minioChannel.getPresignedUrlSafe(documentDetail.getObjectKey(), 3600))
                .fileSize(documentDetail.getFileSize())
                .updatedAt(documentDetail.getUpdatedAt())
                .build();
    }

    private Account getCurrentUser() {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }
}
