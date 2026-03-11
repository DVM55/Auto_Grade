package com.example.Auto_Grade.service;


import com.example.Auto_Grade.dto.req.ExamSessionRequest;
import com.example.Auto_Grade.dto.req.UpdateExamSessionRequest;
import com.example.Auto_Grade.dto.res.ExamSessionResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;


public interface ExamSessionService {

    void createExamSession(Long examId, ExamSessionRequest examSessionRequest);

    void updateExamSession(Long id, UpdateExamSessionRequest updateExamSessionRequest);

    void deleteExamById(Long id);

    PagingResponse<ExamSessionResponse> getExamSessionByExamId(
            Long examId,
            String sessionName,
            int page,
            int size
    );
}
