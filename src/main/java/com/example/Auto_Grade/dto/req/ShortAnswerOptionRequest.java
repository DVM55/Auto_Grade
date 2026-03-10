package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShortAnswerOptionRequest {

    @NotBlank(message = "Đáp án không được để trống")
    private String answer;
}