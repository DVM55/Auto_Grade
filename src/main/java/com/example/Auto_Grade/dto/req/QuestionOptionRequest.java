package com.example.Auto_Grade.dto.req;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionOptionRequest {

    private String optionText;

    private Boolean isCorrect = false;
}
