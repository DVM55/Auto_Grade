package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.QuizAccessType;


import com.example.Auto_Grade.enums.QuizStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;



@Data
@Builder
public class QuizResponse {
    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer maxAttempts;
    private LocalDateTime createdAt;
    private Integer questionCount;
    private QuizAccessType quizAccessType;
    private QuizStatus quizStatus;
}

