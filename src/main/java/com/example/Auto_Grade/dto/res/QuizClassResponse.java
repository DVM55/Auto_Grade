package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizClassResponse {
    private Long id;
    private String title;
}
