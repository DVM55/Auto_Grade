package com.example.Auto_Grade.dto.res;

import lombok.*;
import java.io.Serializable;

@Getter @Setter
@AllArgsConstructor
@Builder
public class ApiResponse<T> implements Serializable {
    private int code;
    private String message;
    private T data;
}
