package com.example.Auto_Grade.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class SaveAnswerRequest {
    private Long questionId;
    private String answerText;          // SHORT_ANSWER
    private List<Long> selectedOptionIds; // SINGLE / MULTIPLE_CHOICE
}
