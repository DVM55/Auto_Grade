package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizQuestionConfigRequest {
    private Long categoryQuestionId;
    private Long groupQuestionId;

    @NotNull(message = "Số lượng câu hỏi không được để trống")
    @Min(value = 1, message = "Số lượng câu hỏi phải lớn hơn 0")
    private Integer quantity;
}
