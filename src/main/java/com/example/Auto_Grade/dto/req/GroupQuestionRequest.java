package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupQuestionRequest {

    @NotBlank(message = "Tên nhóm câu hỏi không được để trống")
    private String name;
}

