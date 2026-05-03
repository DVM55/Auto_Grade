package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizForMemberResponse {
    private Long id;
    private String quizTitle;
    private String quizDescription;
    private boolean isSubmitted;
}
