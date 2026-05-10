package com.example.Auto_Grade.dto.req;

import com.example.Auto_Grade.enums.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class GenerateQuestionMultipartRequest {

    private MultipartFile file;

    @NotNull(message = "So luong cau hoi khong duoc de trong")
    @Min(value = 1, message = "So luong cau hoi toi thieu la 1")
    @Max(value = 20, message = "So luong cau hoi toi da la 20")
    private Integer quantity;

    private String requirement;

    @NotNull(message = "Loai cau hoi khong duoc de trong")
    private QuestionType questionType;
}
