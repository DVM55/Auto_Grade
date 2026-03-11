package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ExamRequest;
import com.example.Auto_Grade.dto.res.ExamResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;

public interface ExamService {
    void deleteExamById(Long id);

    void createExam(ExamRequest examRequest);

    void updateExam(Long id, ExamRequest examRequest);

    PagingResponse<ExamResponse> getExamsByCreator(String name, int page, int size);
}
