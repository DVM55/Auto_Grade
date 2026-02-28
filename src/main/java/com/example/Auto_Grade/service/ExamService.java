package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ExamRequest;
import com.example.Auto_Grade.dto.res.ExamResponse;
import org.springframework.data.domain.Page;

public interface ExamService {
    void deleteExamById(Long id);

    void createExam(ExamRequest examRequest);

    ExamResponse updateExam(Long id, ExamRequest examRequest);

    Page<ExamResponse> getExams(
            String name,
            int page,
            int size
    );
}
