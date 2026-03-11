package com.example.Auto_Grade.integration.minio.dto;

import lombok.Data;

@Data
public class PresignPutRequest {
    private String fileName;
    private String contentType;
}
