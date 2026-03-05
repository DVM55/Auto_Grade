package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentRequest {
    private String objectKey;
    private String fileName;
    private String contentType;
}
