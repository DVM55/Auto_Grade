package com.example.Auto_Grade.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;


import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class QuizAttemptResultResponse {
    private Long id;
    private Integer correctCount;
    private Integer totalQuestions;
    private Double totalScore;
    private LocalDateTime submittedAt;
    private Boolean allowReview;
}
