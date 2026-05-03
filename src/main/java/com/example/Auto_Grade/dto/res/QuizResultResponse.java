package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QuizResultResponse {
    private Long attemptId;
    private Integer correctCount;
    private Integer totalQuestions;
    private Double totalScore;
    private LocalDateTime submittedAt;
    private String submittedByName;
    private String submittedByEmail;
}
