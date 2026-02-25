package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassResponse {
    private Long id;
    private String classCode;
    private String title;
    private String description;
    private Long memberCount;
}
