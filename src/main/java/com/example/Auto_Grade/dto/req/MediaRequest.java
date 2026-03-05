package com.example.Auto_Grade.dto.req;

import lombok.Data;

@Data
public class MediaRequest {
    private String objectKey;
    private String fileName;
    private String contentType;
}
