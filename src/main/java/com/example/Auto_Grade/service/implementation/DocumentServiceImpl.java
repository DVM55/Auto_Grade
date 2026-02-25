package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.DocumentRequest;
import com.example.Auto_Grade.dto.res.DocumentResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.Class;
import com.example.Auto_Grade.entity.Document;

import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.ClassRepository;
import com.example.Auto_Grade.repository.DocumentRepository;
import com.example.Auto_Grade.service.DocumentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;
    private final AccountRepository accountRepository;
    private final ClassRepository classRepository;

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
    public DocumentResponse createDocument(DocumentRequest request, Long classId){
        Account user = getCurrentUser();

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp với id: " + classId));

        if (!clazz.getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền tạo thư mục cho lớp này");
        }

        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setClassEntity(clazz);

        documentRepository.save(document);

        return toResponse(document);
    }

    @Override
    public DocumentResponse updateDocument(DocumentRequest request, Long documentId) {
        Account user = getCurrentUser();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài liệu với id: " + documentId));

        if (!document.getClassEntity().getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa đổi thư mục này");
        }

        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());

        documentRepository.save(document);

        return toResponse(document);
    }

    @Override
    public Page<DocumentResponse> getDocuments(Long classId, int page, int size) {

        Long userId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        boolean hasAccess = classRepository.hasAccessToClass(classId, userId);

        if (!hasAccess) {
            throw new AccessDeniedException("Bạn không có quyền xem tài liệu của lớp này");
        }

        Pageable pageable = PageRequest.of(page, size);

        return documentRepository.findByClassId(classId, pageable)
                .map(this::toResponse);
    }

    public DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .updatedAt(document.getUpdatedAt())
                .build();
    }


    private Account getCurrentUser() {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }
}
