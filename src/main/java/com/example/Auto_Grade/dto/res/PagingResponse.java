package com.example.Auto_Grade.dto.res;

import lombok.*;
import java.io.Serializable;
import java.util.List;

@Getter @Setter
@AllArgsConstructor
@Builder
public class PagingResponse<T> implements Serializable {
    private int code;
    private String message;
    private List<T> data;
    private MetaResponse meta;
}
