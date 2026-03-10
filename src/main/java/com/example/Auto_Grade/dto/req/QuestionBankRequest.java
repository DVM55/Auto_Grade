package com.example.Auto_Grade.dto.req;

import com.example.Auto_Grade.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionBankRequest {

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    @NotNull(message = "Loại câu hỏi không được để trống")
    private QuestionType questionType;

    private String mediaObjectKey;
    private String mediaContentType;

    private Long groupQuestionId;
    private Long categoryQuestionId;

    // Dùng cho SINGLE_CHOICE / MULTIPLE_CHOICE
    @Valid
    private List<QuestionOptionRequest> options;

    // Dùng cho SHORT_ANSWER – danh sách đáp án đúng (ít nhất 1)
    @Valid
    private List<ShortAnswerOptionRequest> correctAnswers;
}

