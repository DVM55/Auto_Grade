package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.MediaType;
import com.example.Auto_Grade.enums.QuestionType;
import lombok.Builder;
import lombok.Data;


import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuestionBankResponse {
    private Long id;
    private String content;
    private QuestionType questionType;
    private String mediaUrl;
    private MediaType mediaType;
    private String groupQuestionName;
    private String categoryQuestionName;
    // Cho SINGLE_CHOICE / MULTIPLE_CHOICE
    private List<QuestionOptionResponse> options;
    // Cho SHORT_ANSWER
    private List<ShortAnswerOptionResponse> correctAnswers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

