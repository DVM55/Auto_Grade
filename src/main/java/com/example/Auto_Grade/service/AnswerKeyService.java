package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.UpdateAnswerKeyRequest;
import com.example.Auto_Grade.dto.res.AnswerKeyResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AnswerKeyService {
    void createAnswerKey(Long examSessionId, MultipartFile file);

    void deleteAllAnswerKeysByExamSessionId(Long examSessionId);

    void deleteAnswerKeyById(Long id);

    List<AnswerKeyResponse> getAllByExamSessionId(Long examSessionId);

    void updateAnswerKey(
            Long answerKeyId,
            UpdateAnswerKeyRequest request
    );

}
