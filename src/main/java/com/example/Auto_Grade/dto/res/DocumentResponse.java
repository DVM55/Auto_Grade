package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.FileType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private String fileUrl;
    private String fileName;
    private String contentType;
    private LocalDateTime updatedAt;
}
