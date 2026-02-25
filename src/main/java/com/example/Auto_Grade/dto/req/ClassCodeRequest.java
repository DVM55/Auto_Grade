package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassCodeRequest {
    @NotBlank(message = "Mã lớp không được để trống")
    private String classCode;
}
