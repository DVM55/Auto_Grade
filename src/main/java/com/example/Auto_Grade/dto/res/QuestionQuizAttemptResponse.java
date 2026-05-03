package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.MediaType;
import com.example.Auto_Grade.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QuestionQuizAttemptResponse {
    private Long id;
    private String content;
    private QuestionType questionType;
    private String mediaUrl;
    private MediaType mediaType;
    private List<OptionResponse> options;
    private List<Long> selectedOptionIds;
    private String answeredText;
}
