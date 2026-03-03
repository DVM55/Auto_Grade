package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExamSessionResponse {
    private Long id;
    private String sessionName;
    private String gradeType;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
