package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;
}
