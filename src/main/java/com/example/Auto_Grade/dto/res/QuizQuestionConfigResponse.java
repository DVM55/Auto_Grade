package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizQuestionConfigResponse {
    private Long id;
    private Long categoryQuestionId;
    private String categoryQuestionName;
    private Long groupQuestionId;
    private String groupQuestionName;
    private Integer quantity;
}
