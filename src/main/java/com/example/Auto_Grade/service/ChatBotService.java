package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ChatRequest;

import com.example.Auto_Grade.dto.res.ChatMessageResponse;

import java.util.List;

public interface ChatBotService {
    ChatMessageResponse chat(ChatRequest request);

    List<ChatMessageResponse> getMessages(String conversationId);

    void deleteConversation(String conversationId);
}
