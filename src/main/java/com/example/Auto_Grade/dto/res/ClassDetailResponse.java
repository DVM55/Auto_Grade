package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.JoinStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassDetailResponse {
    private Long id;
    private String classCode;
    private String title;
    private String description;
    private Long memberCount;
    private JoinStatus joinStatus;
}
