package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.MediaType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MediaResponse {
    private Long id;
    private String fileUrl;
    private String fileName;
    private String objectKey;
    private String contentType;
    private MediaType mediaType;
    private LocalDateTime updatedAt;
}
