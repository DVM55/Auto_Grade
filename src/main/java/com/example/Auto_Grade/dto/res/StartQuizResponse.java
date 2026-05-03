package com.example.Auto_Grade.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class StartQuizResponse {
    private Long attemptId;
    private String quizTitle;
    private Integer durationMinutes;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private List<QuestionQuizAttemptResponse> questions;
}
