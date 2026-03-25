package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.QuestionBankRequest;
import com.example.Auto_Grade.dto.req.UpdateQuestionRequest;
import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.dto.res.QuestionBankResponse;
import com.example.Auto_Grade.enums.QuestionFilterMode;
import com.example.Auto_Grade.enums.QuestionType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {

    void updateQuestion(Long questionId, QuestionBankRequest request);

    void deleteQuestion(Long questionId);
    void createQuestionBank(List<QuestionBankRequest> requests);

    void deleteAllQuestionByCreatorId();

    PagingResponse<QuestionBankResponse> getQuestion(
            String content,
            Long categoryId,
            Long groupId,
            QuestionType questionType,
            QuestionFilterMode questionFilterMode,
            int page,
            int size
    );

    void deleteQuestionByIds(List<Long> questionIds);

    void updateQuestionByIds(UpdateQuestionRequest request);

    QuestionBankResponse getQuestionBankById(Long questionId);

    List<QuestionBankRequest> importWord(MultipartFile file);

    List<QuestionBankRequest> importExcel(MultipartFile file);
}

