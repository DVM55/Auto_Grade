package com.example.Auto_Grade.dto.req;

import lombok.Data;
import java.util.List;

@Data
public class UpdateQuestionRequest {
    private Long categoryId;
    private Long groupId;
    private List<Long> questionId;
}