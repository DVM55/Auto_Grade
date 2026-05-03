package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.MediaType;
import com.example.Auto_Grade.enums.QuestionType;
import lombok.Builder;
import lombok.Data;


import java.util.List;

@Data
@Builder
public class QuestionResponse {
    private Long id;
    private String content;
    private QuestionType questionType;
    private String mediaUrl;
    private MediaType mediaType;
    private Double score;
    private String mediaObjectKey;
    private List<QuestionOptionResponse> options;
    private List<ShortAnswerOptionResponse> correctAnswers;
}
