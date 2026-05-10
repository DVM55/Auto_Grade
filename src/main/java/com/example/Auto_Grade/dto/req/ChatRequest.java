package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    @NotBlank(message = "Noi dung chat khong duoc de trong")
    @Size(max = 4000, message = "Noi dung chat khong duoc vuot qua 4000 ky tu")
    private String message;
}
