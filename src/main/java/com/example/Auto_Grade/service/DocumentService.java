package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.DocumentRequest;
import com.example.Auto_Grade.dto.req.UpdateDocumentRequest;
import com.example.Auto_Grade.dto.res.DocumentResponse;
import com.example.Auto_Grade.entity.Document;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DocumentService {
    void delete(Long documentId);

    void createDocument(List<DocumentRequest> requests, Long classId);

    void updateDocument(UpdateDocumentRequest request, Long documentId);

    Page<DocumentResponse> getDocuments(Long classId, int page, int size);

}
