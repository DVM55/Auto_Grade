package com.example.Auto_Grade.dto.res;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnswerKeyResponse {

    private Long id;
    private String paperCode;

    private Map<String, String> part1;
    private Map<String, String> part2;
    private Map<String, String> part3;
}
