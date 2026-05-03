package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.MediaType;
import com.example.Auto_Grade.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QuizAttemptAnswerResponse {
    private Long id;
    private String content;
    private QuestionType questionType;
    private String mediaUrl;
    private MediaType mediaType;
    private List<QuestionOptionResponse> options;
    // Cho SHORT_ANSWER
    private List<ShortAnswerOptionResponse> correctAnswers;
    private List<Long> selectedOptionIds;
    private String answeredText;
    private Boolean isCorrect;
    private Double score;
}
