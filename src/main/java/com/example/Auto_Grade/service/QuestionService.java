package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.QuestionBankRequest;
import com.example.Auto_Grade.dto.res.QuestionBankResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {

    void updateQuestion(Long questionId, QuestionBankRequest request);


    void deleteQuestion(Long questionId);
    void createQuestionBank(List<QuestionBankRequest> requests);

    void deleteAllQuestionByCreatorId();

    Page<QuestionBankResponse> getQuestionBank(
            Long categoryId,
            Long groupId,
            int page,
            int size
    );

    QuestionBankResponse getQuestionBankById(Long questionId);

    List<QuestionBankRequest> importWord(MultipartFile file);

    List<QuestionBankRequest> importExcel(MultipartFile file);
}

