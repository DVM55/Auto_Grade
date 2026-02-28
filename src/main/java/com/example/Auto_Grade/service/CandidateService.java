package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.CandidateRequest;
import com.example.Auto_Grade.dto.res.CandidateResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface CandidateService {
    void importCandidates(Long examId, MultipartFile file);
    CandidateResponse updateCandidate(Long id, CandidateRequest candidateRequest);
    void deleteCandidateById(Long id);
    void deleteAllCandidateByExamId(Long id);
    Page<CandidateResponse> getCandidatesByExamId(
            Long id,
            String fullName,
            String candidateNumber,
            String examRoom,
            String note,
            String className,
            int page,
            int size
    );
    byte[] exportCandidatesToExcel(Long examId);
}
