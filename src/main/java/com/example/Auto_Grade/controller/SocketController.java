package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.AnswerRequest;
import com.example.Auto_Grade.service.QuizAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SocketController {
    private final QuizAttemptService quizAttemptService;

    @MessageMapping("/quiz/answer")
    public void saveAnswer(@Payload AnswerRequest req) {

        quizAttemptService.saveAnswer(req);
    }
}
