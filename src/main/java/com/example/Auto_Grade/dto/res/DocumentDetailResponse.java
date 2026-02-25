package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class DocumentDetailResponse {
    private Long id;
    private String fileName;
    private String contentType;
    private String fileUrl;
    private Long fileSize;
    private LocalDateTime updatedAt;
}
