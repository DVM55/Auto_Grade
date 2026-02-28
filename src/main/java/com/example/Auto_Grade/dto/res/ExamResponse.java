package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;


import java.time.LocalDateTime;

@Data
@Builder
public class ExamResponse {
    private Long id;
    private String name;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
