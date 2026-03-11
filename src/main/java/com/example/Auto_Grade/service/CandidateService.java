package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.CandidateRequest;
import com.example.Auto_Grade.dto.res.CandidateResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CandidateService {
    void importCandidates(Long examId, MultipartFile file);
    void updateCandidate(Long id, CandidateRequest candidateRequest);
    void deleteCandidateById(Long id);
    void deleteAllCandidateByExamId(Long id);
    PagingResponse<CandidateResponse> getCandidatesByExamId(
            Long examId,
            String fullName,
            String candidateNumber,
            String examRoom,
            String note,
            String className,
            int page,
            int size
    );
    byte[] exportCandidatesToExcel(Long examId);

    byte[] exportCandidatesGroupByExamRoom(Long examId);
}
