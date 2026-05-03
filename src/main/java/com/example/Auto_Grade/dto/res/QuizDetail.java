package com.example.Auto_Grade.dto.res;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QuizDetail {
    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer maxAttempts;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
