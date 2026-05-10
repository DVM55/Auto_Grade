package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.entity.Question;
import com.example.Auto_Grade.entity.QuestionOption;
import com.example.Auto_Grade.entity.ShortAnswerOption;
import com.example.Auto_Grade.enums.QuestionType;
import com.example.Auto_Grade.repository.QuestionRepository;
import com.example.Auto_Grade.service.QuestionExplanationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionExplanationServiceImpl implements QuestionExplanationService {

    private final QuestionRepository questionRepository;
    private final RestClient chatBotRestClient;

    @Value("${chatbot.agent.access-key}")
    private String agentAccessKey;

    @Override
    @Transactional
    public String explainQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Khong tim thay cau hoi voi id: " + questionId));

        if (StringUtils.hasText(question.getExplanation())) {
            return question.getExplanation();
        }

        String explanation = callExplanationAgent(question);
        question.setExplanation(explanation);
        questionRepository.save(question);

        return explanation;
    }

    private String callExplanationAgent(Question question) {
        AgentChatResponse response = chatBotRestClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + requireAgentAccessKey())
                .body(new AgentChatRequest(List.of(
                        new AgentMessage("user", buildPrompt(question))
                )))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, clientResponse) -> {
                    String errorBody = readAgentErrorBody(clientResponse);
                    throw new ResponseStatusException(
                            clientResponse.getStatusCode(),
                            "Khong the goi chatbot agent de giai thich cau hoi"
                                    + (StringUtils.hasText(errorBody) ? ": " + errorBody : "")
                    );
                })
                .body(AgentChatResponse.class);

        return extractAnswer(response);
    }

    private String buildPrompt(Question question) {
        return """
        Bạn là giáo viên đang giải thích bài cho học sinh sau khi làm xong bài kiểm tra.

        Nhiệm vụ: Giải thích ngắn gọn, tự nhiên để học sinh hiểu bản chất câu hỏi và vì sao đáp án đúng là đúng.

        Quy tắc bắt buộc:
        - Chỉ trả về văn bản thuần, không dùng markdown, JSON, LaTeX hay bất kỳ ký hiệu định dạng nào.
        - Viết như giáo viên đang nói chuyện trực tiếp với học sinh, tự nhiên và thân thiện.
        - Không đề cập đến các field kỹ thuật như isCorrect, optionId, questionType,...
        - Không phân tích từng đáp án sai — chỉ nhắc đến phương án dễ nhầm nếu thực sự cần thiết.
        - Không bịa thêm thông tin ngoài nội dung được cung cấp.
        - Câu dễ: giải thích 1–2 câu là đủ. Câu khó hoặc cần suy luận: có thể dài hơn nhưng vẫn súc tích.
        - Không mở đầu bằng "Đáp án đúng là..." một cách cứng nhắc — hãy đi thẳng vào giải thích.

        Theo loại câu hỏi:
        - SINGLE_CHOICE: Giải thích vì sao lựa chọn đúng phù hợp nhất với câu hỏi.
        - MULTIPLE_CHOICE: Giải thích điều kiện hoặc lý do khiến các đáp án được chọn đều đúng.
        - SHORT_ANSWER: Nêu ý chính cần có trong câu trả lời và cách xác định đáp án đúng.

        Loại câu hỏi: %s

        Nội dung câu hỏi:
        %s

        Thông tin đáp án:
        %s
        """.formatted(
                question.getQuestionType(),
                question.getContent(),
                answerContext(question)
        );
    }

    private String answerContext(Question question) {
        if (question.getQuestionType() == QuestionType.SHORT_ANSWER) {
            return shortAnswerContext(question);
        }

        return choiceAnswerContext(question);
    }

    private String choiceAnswerContext(Question question) {
        StringBuilder builder = new StringBuilder("question.options:\n");

        List<QuestionOption> options = question.getOptions();
        for (QuestionOption option : options) {
            builder.append("- optionText: ")
                    .append(option.getOptionText())
                    .append('\n')
                    .append("  isCorrect: ")
                    .append(Boolean.TRUE.equals(option.getIsCorrect()))
                    .append('\n');
        }

        return builder.toString();
    }

    private String shortAnswerContext(Question question) {
        StringBuilder builder = new StringBuilder("question.shortAnswerOptions:\n");

        for (ShortAnswerOption answer : question.getShortAnswerOptions()) {
            builder.append("- ")
                    .append("answerText: ")
                    .append(answer.getAnswerText())
                    .append('\n');
        }

        return builder.toString();
    }

    private String extractAnswer(AgentChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(502),
                    "Chatbot agent khong tra ve giai thich");
        }

        AgentMessage message = response.choices().getFirst().message();
        if (message == null || !StringUtils.hasText(message.content())) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(502),
                    "Noi dung giai thich chatbot tra ve bi trong");
        }

        return message.content().trim();
    }

    private String requireAgentAccessKey() {
        if (!StringUtils.hasText(agentAccessKey)) {
            throw new AuthenticationCredentialsNotFoundException("Thieu cau hinh AGENT_ACCESS_KEY");
        }

        return agentAccessKey;
    }

    private String readAgentErrorBody(ClientHttpResponse clientResponse) {
        try {
            return new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private record AgentChatRequest(List<AgentMessage> messages) {
    }

    private record AgentChatResponse(List<AgentChoice> choices) {
    }

    private record AgentChoice(AgentMessage message) {
    }

    private record AgentMessage(String role, String content) {
    }
}
