package com.example.Auto_Grade.dto.req;

import com.example.Auto_Grade.enums.FileType;
import lombok.Data;

@Data
public class DocumentDetailRequest {
    private String objectKey;
    private String fileName;
    private String contentType;
    private Long fileSize;
}