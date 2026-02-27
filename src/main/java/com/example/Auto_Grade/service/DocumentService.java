package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.DocumentRequest;
import com.example.Auto_Grade.dto.res.DocumentResponse;
import com.example.Auto_Grade.entity.Document;
import org.springframework.data.domain.Page;

public interface DocumentService {
    void delete(Long documentId);

    DocumentResponse createDocument(DocumentRequest request, Long classId);

    DocumentResponse updateDocument(DocumentRequest request, Long documentId);

    Page<DocumentResponse> getDocuments(Long classId, int page, int size);

    Document getDocumentById(Long documentId);

}
