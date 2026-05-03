package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.QuizAccessType;
import com.example.Auto_Grade.enums.QuizStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizDetailResponse {
    private Long id;
    private String quizCode;
    private String title;
    private String description;
    private Double totalScore;
    private Integer durationMinutes;
    private Integer maxAttempts;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allowReview;
    private QuizStatus quizStatus;
    private QuizAccessType quizAccessType;
    private Boolean autoScore;
    private List<QuestionResponse> questions;
    private List<QuizClassResponse> classes;
    private Boolean isRandom;
    private List<QuizQuestionConfigResponse> randomConfigs; // trả về config khi getDetail
}
