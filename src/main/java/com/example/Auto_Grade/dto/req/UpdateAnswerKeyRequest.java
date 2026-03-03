package com.example.Auto_Grade.dto.req;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateAnswerKeyRequest {
    private String paperCode;

    private Map<String, String> part1;
    private Map<String, String> part2;
    private Map<String, String> part3;
}
