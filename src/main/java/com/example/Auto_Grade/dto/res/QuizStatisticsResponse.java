package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizStatisticsResponse {
    private long totalAttempts;
    private double averageScore;
    private String averageTime;
    private boolean randomQuestions;
    private long excellent;
    private long good;
    private long average;
    private long weak;
}
