package com.example.Auto_Grade.dto.req;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateAvatarRequest {
    private MultipartFile file;
}