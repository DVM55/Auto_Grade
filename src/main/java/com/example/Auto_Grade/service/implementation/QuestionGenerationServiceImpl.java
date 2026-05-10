package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.GenerateQuestionMultipartRequest;
import com.example.Auto_Grade.dto.req.QuestionBankRequest;
import com.example.Auto_Grade.dto.req.QuestionOptionRequest;
import com.example.Auto_Grade.dto.req.ShortAnswerOptionRequest;
import com.example.Auto_Grade.enums.QuestionType;
import com.example.Auto_Grade.service.DocumentExtractionService;
import com.example.Auto_Grade.service.QuestionGenerationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionGenerationServiceImpl implements QuestionGenerationService {

    private static final Pattern MARKDOWN_CODE_BLOCK =
            Pattern.compile("```(?:json)?\\s*(.*?)\\s*```", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final DocumentExtractionService documentExtractionService;
    private final RestClient chatBotRestClient;
    private final ObjectMapper objectMapper;

    @Value("${chatbot.agent.access-key}")
    private String agentAccessKey;

    public QuestionGenerationServiceImpl(
            DocumentExtractionService documentExtractionService,
            RestClient chatBotRestClient,
            ObjectMapper objectMapper
    ) {
        this.documentExtractionService = documentExtractionService;
        this.chatBotRestClient = chatBotRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<QuestionBankRequest> generateQuestions(GenerateQuestionMultipartRequest request) {
        boolean fileProvided = hasFile(request.getFile());
        validateGenerationInput(fileProvided, request);

        String documentText = fileProvided ? documentExtractionService.extractText(request.getFile()) : null;
        String modelResponse = callQuestionAgent(request, documentText);

        return parseQuestions(modelResponse, request.getQuantity(), request.getQuestionType());
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private void validateGenerationInput(boolean fileProvided, GenerateQuestionMultipartRequest request) {
        if (!fileProvided && !StringUtils.hasText(request.getRequirement())) {
            throw new IllegalArgumentException("Requirement khong duoc de trong khi khong gui file");
        }
    }

    private String callQuestionAgent(GenerateQuestionMultipartRequest request, String documentText) {
        AgentChatResponse response = chatBotRestClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + requireAgentAccessKey())
                .body(new AgentChatRequest(List.of(
                        new AgentMessage("user", buildQuestionPrompt(request, documentText))
                )))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, clientResponse) -> {
                    String errorBody = readAgentErrorBody(clientResponse);
                    throw new ResponseStatusException(
                            clientResponse.getStatusCode(),
                            "Khong the goi chatbot agent de sinh cau hoi"
                                    + (StringUtils.hasText(errorBody) ? ": " + errorBody : "")
                    );
                })
                .body(AgentChatResponse.class);

        return extractAnswer(response);
    }

    private String buildQuestionPrompt(GenerateQuestionMultipartRequest request, String documentText) {
        return systemPrompt() + "\n\n" + userPrompt(request, documentText);
    }

    private String systemPrompt() {
        return """
                You generate Vietnamese quiz questions from study material.
                Return only valid JSON, with no markdown and no explanation.
                The JSON must be an array. Each array item must have this shape:
                {
                  "questionType": "SINGLE_CHOICE | MULTIPLE_CHOICE | SHORT_ANSWER",
                  "content": "question text",
                  "options": [
                    {"optionText": "answer A", "isCorrect": true},
                    {"optionText": "answer B", "isCorrect": false},
                    {"optionText": "answer C", "isCorrect": false},
                    {"optionText": "answer D", "isCorrect": false}
                  ],
                  "correctAnswers": [
                    {"answer": "accepted short answer"}
                  ]
                }
                Language rules:
                            - The entire question content and all answer options must use ONE consistent language only.
                            - If the topic is English grammar, vocabulary, pronunciation, or translation:
                              + The question sentence and answer options must be fully in English.
                              + Do NOT mix Vietnamese and English in the same sentence.
                Rules:
                - Create exactly the requested number of questions.
                - Use exactly the requested question type for every item.
                - SINGLE_CHOICE must have exactly 4 options and exactly 1 correct option.
                - MULTIPLE_CHOICE must have at least 4 options and at least 2 correct options.
                - SHORT_ANSWER must not use options and must have at least 1 correctAnswers item.
                - If Source mode is DOCUMENT_PROVIDED, base every question only on the provided document text.
                - If Source mode is DOCUMENT_PROVIDED and Requirement is provided, use it as an extra constraint.
                - If Source mode is NO_DOCUMENT, generate questions from Requirement.
                - Do not create duplicate questions.
                - For every choice question, the 4 optionText values must be unique after trimming whitespace and ignoring case.
                - Do not create options with the same meaning, even if the wording is slightly different.
                - Do not use vague options such as "All of the above", "None of the above", "Both A and B", or equivalent Vietnamese phrases.
                - Before returning JSON, self-check every question and rewrite any question that has duplicate or near-duplicate options.
                """;
    }

    private String userPrompt(GenerateQuestionMultipartRequest request, String documentText) {
        String requirement = StringUtils.hasText(request.getRequirement())
                ? request.getRequirement().trim()
                : "None";
        boolean hasDocument = StringUtils.hasText(documentText);

        return """
                Quantity: %d
                Requirement: %s
                Question type: %s
                Source mode: %s

                Document text:
                %s
                """.formatted(
                request.getQuantity(),
                requirement,
                request.getQuestionType(),
                hasDocument ? "DOCUMENT_PROVIDED" : "NO_DOCUMENT",
                hasDocument ? documentText : "No document was provided."
        );
    }

    private List<QuestionBankRequest> parseQuestions(
            String modelResponse,
            int requestedQuantity,
            QuestionType requestedQuestionType
    ) {
        if (!StringUtils.hasText(modelResponse)) {
            throw new IllegalArgumentException("Model returned an empty response");
        }

        List<GeneratedQuestion> generatedQuestions = readGeneratedQuestions(modelResponse);
        if (generatedQuestions.isEmpty()) {
            throw new IllegalArgumentException("Model did not generate any questions");
        }

        if (generatedQuestions.size() > requestedQuantity) {
            generatedQuestions = generatedQuestions.subList(0, requestedQuantity);
        }

        List<QuestionBankRequest> result = new ArrayList<>();
        for (int i = 0; i < generatedQuestions.size(); i++) {
            result.add(toQuestionBankRequest(generatedQuestions.get(i), i + 1, requestedQuestionType));
        }

        return result;
    }

    private List<GeneratedQuestion> readGeneratedQuestions(String modelResponse) {
        String jsonPayload = extractJsonPayload(modelResponse);

        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            JsonNode questionArray = root.isArray() ? root : root.get("questions");

            if (questionArray == null || !questionArray.isArray()) {
                throw new IllegalArgumentException("Model JSON must be an array of questions");
            }

            return objectMapper.convertValue(
                    questionArray,
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Model did not return valid question JSON", e);
        }
    }

    private String extractJsonPayload(String modelResponse) {
        String content = modelResponse.trim();
        Matcher codeBlockMatcher = MARKDOWN_CODE_BLOCK.matcher(content);
        if (codeBlockMatcher.find()) {
            content = codeBlockMatcher.group(1).trim();
        }

        int arrayStart = content.indexOf('[');
        int arrayEnd = content.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return content.substring(arrayStart, arrayEnd + 1);
        }

        int objectStart = content.indexOf('{');
        int objectEnd = content.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return content.substring(objectStart, objectEnd + 1);
        }

        throw new IllegalArgumentException("Model response does not contain JSON");
    }

    private QuestionBankRequest toQuestionBankRequest(
            GeneratedQuestion generatedQuestion,
            int questionIndex,
            QuestionType requestedQuestionType
    ) {
        if (!StringUtils.hasText(generatedQuestion.content())) {
            throw new IllegalArgumentException("Generated question " + questionIndex + " has empty content");
        }

        QuestionType questionType = generatedQuestion.questionType() == null
                ? requestedQuestionType
                : generatedQuestion.questionType();
        if (questionType != requestedQuestionType) {
            throw new IllegalArgumentException("Generated question " + questionIndex + " has invalid question type");
        }

        if (questionType == QuestionType.SHORT_ANSWER) {
            return toShortAnswerQuestionBankRequest(generatedQuestion, questionIndex);
        }

        List<GeneratedOption> generatedOptions = generatedQuestion.options();
        if (generatedOptions == null || generatedOptions.size() < 4) {
            throw new IllegalArgumentException("Generated question " + questionIndex + " must have at least 4 options");
        }

        if (questionType == QuestionType.SINGLE_CHOICE && generatedOptions.size() != 4) {
            throw new IllegalArgumentException("Generated question " + questionIndex + " must have exactly 4 options");
        }

        List<QuestionOptionRequest> options = new ArrayList<>();
        Set<String> seenOptions = new HashSet<>();
        int correctCount = 0;

        for (int i = 0; i < generatedOptions.size(); i++) {
            GeneratedOption option = generatedOptions.get(i);
            if (option == null || !StringUtils.hasText(option.optionText())) {
                throw new IllegalArgumentException("Generated question " + questionIndex + " has an empty option");
            }

            String optionText = option.optionText().trim();
            String optionKey = optionText.toLowerCase(Locale.ROOT);
            if (!seenOptions.add(optionKey)) {
                throw new IllegalArgumentException("Generated question " + questionIndex + " has duplicate options");
            }

            boolean isCorrect = resolveCorrectOption(generatedQuestion, option, i);
            if (isCorrect) {
                correctCount++;
            }

            options.add(QuestionOptionRequest.builder()
                    .optionText(optionText)
                    .isCorrect(isCorrect)
                    .build());
        }

        if (questionType == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            throw new IllegalArgumentException("Generated question " + questionIndex + " must have exactly 1 correct option");
        }

        if (questionType == QuestionType.MULTIPLE_CHOICE && correctCount < 2) {
            throw new IllegalArgumentException("Generated question " + questionIndex + " must have at least 2 correct options");
        }

        return QuestionBankRequest.builder()
                .content(generatedQuestion.content().trim())
                .questionType(questionType)
                .options(options)
                .build();
    }

    private QuestionBankRequest toShortAnswerQuestionBankRequest(GeneratedQuestion generatedQuestion, int questionIndex) {
        List<GeneratedShortAnswer> generatedAnswers = generatedQuestion.correctAnswers();
        if (generatedAnswers == null || generatedAnswers.isEmpty()) {
            throw new IllegalArgumentException("Generated question " + questionIndex + " must have at least 1 correct answer");
        }

        List<ShortAnswerOptionRequest> correctAnswers = new ArrayList<>();
        Set<String> seenAnswers = new HashSet<>();

        for (GeneratedShortAnswer generatedAnswer : generatedAnswers) {
            if (generatedAnswer == null || !StringUtils.hasText(generatedAnswer.answer())) {
                throw new IllegalArgumentException("Generated question " + questionIndex + " has an empty correct answer");
            }

            String answer = generatedAnswer.answer().trim();
            if (!seenAnswers.add(answer.toLowerCase(Locale.ROOT))) {
                continue;
            }

            ShortAnswerOptionRequest answerRequest = new ShortAnswerOptionRequest();
            answerRequest.setAnswer(answer);
            correctAnswers.add(answerRequest);
        }

        if (correctAnswers.isEmpty()) {
            throw new IllegalArgumentException("Generated question " + questionIndex + " must have at least 1 correct answer");
        }

        return QuestionBankRequest.builder()
                .content(generatedQuestion.content().trim())
                .questionType(QuestionType.SHORT_ANSWER)
                .correctAnswers(correctAnswers)
                .build();
    }

    private boolean resolveCorrectOption(GeneratedQuestion question, GeneratedOption option, int optionIndex) {
        if (option.isCorrect() != null) {
            return option.isCorrect();
        }

        if (!StringUtils.hasText(question.correctAnswer())) {
            return false;
        }

        String correctAnswer = question.correctAnswer().trim();
        String optionLabel = String.valueOf((char) ('A' + optionIndex));

        return correctAnswer.equalsIgnoreCase(optionLabel)
                || correctAnswer.equalsIgnoreCase(option.optionText().trim());
    }

    private String extractAnswer(AgentChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(502),
                    "Chatbot agent khong tra ve cau hoi");
        }

        AgentMessage message = response.choices().getFirst().message();
        if (message == null || !StringUtils.hasText(message.content())) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(502),
                    "Noi dung cau hoi chatbot tra ve bi trong");
        }

        return message.content();
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

    private record GeneratedQuestion(
            QuestionType questionType,
            String content,
            List<GeneratedOption> options,
            String correctAnswer,
            List<GeneratedShortAnswer> correctAnswers
    ) {
    }

    private record GeneratedOption(
            String optionText,
            Boolean isCorrect
    ) {
    }

    private record GeneratedShortAnswer(
            String answer
    ) {
    }
}
