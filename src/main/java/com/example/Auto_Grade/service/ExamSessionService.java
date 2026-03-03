package com.example.Auto_Grade.service;


import com.example.Auto_Grade.dto.req.ExamSessionRequest;
import com.example.Auto_Grade.dto.req.UpdateExamSessionRequest;
import com.example.Auto_Grade.dto.res.ExamSessionResponse;
import org.springframework.data.domain.Page;


public interface ExamSessionService {

    void createExamSession(Long examId, ExamSessionRequest examSessionRequest);

    ExamSessionResponse updateExamSession(Long id, UpdateExamSessionRequest updateExamSessionRequest);

    void deleteExamById(Long id);

    Page<ExamSessionResponse> getExamSessionByExamId(
            Long examId,
            String sessionName,
            int page,
            int size
    );
}
