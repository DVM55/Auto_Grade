package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionOptionRequest {

    @NotBlank(message = "Nội dung đáp án không được để trống")
    private String optionText;

    private Boolean isCorrect = false;
}
