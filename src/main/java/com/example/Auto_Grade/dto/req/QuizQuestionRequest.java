package com.example.Auto_Grade.dto.req;

import com.example.Auto_Grade.enums.MediaType;
import com.example.Auto_Grade.enums.QuestionType;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizQuestionRequest {

    private String content;

    private QuestionType questionType;

    private Double score;

    private String mediaObjectKey;
    private MediaType mediaType;

    // Dùng cho SINGLE_CHOICE / MULTIPLE_CHOICE
    @Valid
    private List<QuestionOptionRequest> options;

    // Dùng cho SHORT_ANSWER – danh sách đáp án đúng (ít nhất 1)
    @Valid
    private List<ShortAnswerOptionRequest> correctAnswers;
}
