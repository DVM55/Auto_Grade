package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExamRequest {
    @NotBlank(message = "Tên không được để trống")
    private String name;
}
