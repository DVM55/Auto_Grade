package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.GenerateQuestionMultipartRequest;
import com.example.Auto_Grade.dto.req.QuestionBankRequest;

import java.util.List;

public interface QuestionGenerationService {
    List<QuestionBankRequest> generateQuestions(GenerateQuestionMultipartRequest request);
}
