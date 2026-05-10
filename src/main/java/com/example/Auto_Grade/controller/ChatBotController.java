package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.ChatRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;

import com.example.Auto_Grade.dto.res.ChatMessageResponse;
import com.example.Auto_Grade.service.ChatBotService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Validated
public class ChatBotController {

    private final ChatBotService chatBotService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> chat(@Valid @RequestBody ChatRequest request) {
        ChatMessageResponse response = chatBotService.chat(request);

        return ResponseEntity.ok(
                ApiResponse.<ChatMessageResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Chat thành công")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/conversations/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages() {
        List<ChatMessageResponse> response = chatBotService.getMessages(null);

        return ResponseEntity.ok(
                ApiResponse.<List<ChatMessageResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy lịch sử chat thành công")
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/conversations")
    public ResponseEntity<ApiResponse<Void>> deleteConversation() {
        chatBotService.deleteConversation(null);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa lịch sử chat thành công")
                        .data(null)
                        .build()
        );
    }
}
