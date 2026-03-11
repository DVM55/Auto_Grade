package com.example.Auto_Grade.integration.minio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResult {
    private String objectKey;
    private String uploadUrl;
    private String fileName;
    private  String contentType;
}
