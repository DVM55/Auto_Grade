package com.example.Auto_Grade.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class QuestionOptionResponse {
    private Long id;
    private String optionText;
    private Boolean isCorrect;
}
