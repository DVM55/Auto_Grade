package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryQuestionRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;
}
