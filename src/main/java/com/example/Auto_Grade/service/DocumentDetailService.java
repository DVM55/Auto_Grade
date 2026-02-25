package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.DocumentDetailRequest;
import com.example.Auto_Grade.dto.req.UpdateDocumentDetailRequest;
import com.example.Auto_Grade.dto.res.DocumentDetailResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DocumentDetailService {

    List<DocumentDetailResponse> createDocumentDetail(
            Long documentId,
            List<DocumentDetailRequest> requests
    );

    DocumentDetailResponse updateDocumentDetail(
            Long documentDetailId,
            UpdateDocumentDetailRequest requests
    );

    void deleteDocumentDetail(Long documentDetailId);

    Page<DocumentDetailResponse> getDocumentDetails(
            Long documentId,
            int page,
            int size
    );
}
