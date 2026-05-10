package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResponse {
    private String conversationId;
    private String answer;
}
