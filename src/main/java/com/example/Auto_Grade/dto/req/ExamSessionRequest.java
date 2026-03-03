package com.example.Auto_Grade.dto.req;

import com.example.Auto_Grade.enums.GradeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamSessionRequest {
    @NotBlank(message = "Tên không được để trống")
    private String sessionName;

    @NotNull(message = "Loại chấm điểm không được để trống")
    private GradeType gradeType;
}
