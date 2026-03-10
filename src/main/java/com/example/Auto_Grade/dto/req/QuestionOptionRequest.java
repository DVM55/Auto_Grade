package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionOptionRequest {

    @NotBlank(message = "Nội dung đáp án không được để trống")
    private String optionText;

    private Boolean isCorrect = false;
}
