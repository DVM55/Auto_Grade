package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassRequest {
    @NotBlank(message = "Tên lớp không được để trống")
    private String title;

    private String description;
}
