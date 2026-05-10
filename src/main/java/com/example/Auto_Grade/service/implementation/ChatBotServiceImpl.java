package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.ChatRequest;
import com.example.Auto_Grade.dto.res.ChatMessageResponse;
import com.example.Auto_Grade.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    private static final int CONTEXT_MESSAGES = 5;

    private final RestClient chatBotRestClient;
    private final ChatMemoryRepository chatMemoryRepository;

    @Value("${chatbot.agent.access-key}")
    private String agentAccessKey;

    @Override
    public ChatMessageResponse chat(ChatRequest request) {
        String conversationId = currentConversationId();

        // lấy toàn bộ lịch sử
        List<Message> history = chatMemoryRepository.findByConversationId(conversationId);

        // chỉ lấy 5 message gần nhất gửi AI
        List<AgentMessage> agentMessages = new ArrayList<>(toAgentMessages(history));
        agentMessages.add(new AgentMessage("user", request.getMessage()));

        AgentChatResponse response = chatBotRestClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + requireAgentAccessKey())
                .body(new AgentChatRequest(agentMessages))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, clientResponse) -> {
                    throw new ResponseStatusException(
                            clientResponse.getStatusCode(),
                            "Khong the goi chatbot agent"
                    );
                })
                .body(AgentChatResponse.class);

        String answer = extractAnswer(response);

        // 🔥 APPEND TRỰC TIẾP TẠI ĐÂY (không dùng add())
        List<Message> updated = new ArrayList<>(history);
        updated.add(new UserMessage(request.getMessage()));
        updated.add(new AssistantMessage(answer));

        chatMemoryRepository.saveAll(conversationId, updated);

        return ChatMessageResponse.builder()
                .role("ASSISTANT")
                .content(answer)
                .build();
    }

    @Override
    public List<ChatMessageResponse> getMessages(String conversationId) {
        return java.util.Optional
                .ofNullable(chatMemoryRepository.findByConversationId(currentConversationId()))
                .orElse(List.of())
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    public void deleteConversation(String conversationId) {
        chatMemoryRepository.deleteByConversationId(conversationId);
    }

    // chỉ lấy 5 tin gần nhất
    private List<AgentMessage> toAgentMessages(List<Message> messages) {
        return messages.stream()
                .skip(Math.max(0, messages.size() - CONTEXT_MESSAGES))
                .map(message -> new AgentMessage(
                        message.getMessageType().name().toLowerCase(Locale.ROOT),
                        message.getText()
                ))
                .toList();
    }

    private String extractAnswer(AgentChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(502),
                    "Chatbot agent khong tra ve cau tra loi");
        }

        AgentMessage message = response.choices().getFirst().message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(502),
                    "Noi dung tra loi chatbot bi trong");
        }

        return message.content();
    }

    private ChatMessageResponse toMessageResponse(Message message) {
        return ChatMessageResponse.builder()
                .role(message.getMessageType().name())
                .content(message.getText())
                .build();
    }

    private String requireAgentAccessKey() {
        if (agentAccessKey == null || agentAccessKey.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Thieu cau hinh AGENT_ACCESS_KEY");
        }
        return agentAccessKey;
    }

    private String currentConversationId() {
        return currentAccountId().toString();
    }

    private Long currentAccountId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private record AgentChatRequest(List<AgentMessage> messages) {}
    private record AgentChatResponse(List<AgentChoice> choices) {}
    private record AgentChoice(AgentMessage message) {}
    private record AgentMessage(String role, String content) {}
}