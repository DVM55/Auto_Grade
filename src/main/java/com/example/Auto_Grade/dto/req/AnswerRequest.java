package com.example.Auto_Grade.dto.req;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnswerRequest {
    private Long attemptId;
    private Long accountId;
    private Long questionId;
    private String answerText;          // SHORT_ANSWER
    private List<Long> selectedOptionIds;
}
