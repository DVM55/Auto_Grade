package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.DocumentRequest;
import com.example.Auto_Grade.dto.req.UpdateDocumentRequest;
import com.example.Auto_Grade.dto.res.DocumentResponse;
import com.example.Auto_Grade.dto.res.MetaResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.Class;
import com.example.Auto_Grade.entity.Document;

import com.example.Auto_Grade.integration.minio.MinioChannel;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.ClassRepository;
import com.example.Auto_Grade.repository.DocumentRepository;
import com.example.Auto_Grade.service.DocumentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;
    private final AccountRepository accountRepository;
    private final ClassRepository classRepository;
    private final MinioChannel minioChannel;

    @Override
    public void delete(Long documentId) {

        Account user = getCurrentUser();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài liệu với id: " + documentId));

        if (!document.getClassEntity().getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xóa thư mục này");
        }

        documentRepository.delete(document);
    }

    @Override
    public void createDocument(List<DocumentRequest> requests, Long classId){
        Account user = getCurrentUser();

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp với id: " + classId));

        if (!clazz.getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thêm tài liệu cho lớp này");
        }

        List<Document> details = requests.stream()
                .map(req -> Document.builder()
                        .classEntity(clazz)
                        .fileName(req.getFileName())
                        .objectKey(req.getObjectKey())
                        .contentType(req.getContentType())
                        .build()
                )
                .toList();

        documentRepository.saveAll(details);
    }

    @Override
    public void updateDocument(UpdateDocumentRequest request, Long documentId) {
        Account user = getCurrentUser();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài liệu với id: " + documentId));

        if (!document.getClassEntity().getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa đổi tài liệu này");
        }
        document.setFileName(request.getFileName());
        documentRepository.save(document);


    }

    @Override
    public PagingResponse<DocumentResponse> getDocumentsByClassId(Long classId, int page, int size) {
        Long userId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        boolean hasAccess = classRepository.hasAccessToClass(classId, userId);

        if (!hasAccess) {
            throw new AccessDeniedException("Bạn không có quyền xem tài liệu của lớp này");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Document> documentPage = documentRepository.findByClassId(classId, pageable);

        MetaResponse meta = MetaResponse.builder()
                .totalItems(documentPage.getTotalElements())
                .itemCount(documentPage.getNumberOfElements())
                .itemsPerPage(documentPage.getSize())
                .totalPages(documentPage.getTotalPages())
                .currentPage(documentPage.getNumber() + 1)
                .build();

        return PagingResponse.<DocumentResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy danh sách tài liệu thành công")
                .data(documentPage.map(this::toResponse).getContent())
                .meta(meta)
                .build();

    }

    public DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileUrl(minioChannel.getPresignedUrlSafe(document.getObjectKey(), 3600))
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .updatedAt(document.getUpdatedAt())
                .build();
    }


    private Account getCurrentUser() {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại"));
    }
}
