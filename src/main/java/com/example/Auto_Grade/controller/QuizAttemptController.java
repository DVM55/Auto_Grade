package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.SaveAnswerRequest;
import com.example.Auto_Grade.dto.res.*;
import com.example.Auto_Grade.service.QuizAttemptService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz-attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    // ───────────── START QUIZ ─────────────
    @PostMapping("/start/{quizId}")
    public ResponseEntity<ApiResponse<StartQuizResponse>> startQuiz(@PathVariable Long quizId) {
        return ResponseEntity.ok(ApiResponse.<StartQuizResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Bắt đầu làm bài thành công")
                .data(quizAttemptService.startQuiz(quizId))
                .build());
    }

    // ───────────── SUBMIT QUIZ ─────────────
    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ApiResponse<QuizAttemptResultResponse>> submitQuiz(
            @PathVariable Long attemptId,
            @RequestBody List<SaveAnswerRequest> request
    ) {
        return ResponseEntity.ok(ApiResponse.<QuizAttemptResultResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Nộp bài thành công")
                .data(quizAttemptService.submitQuiz(attemptId, request))
                .build());
    }

    // ───────────── GET MY ATTEMPT HISTORY ─────────────
    @GetMapping("/history/{quizId}")
    public ResponseEntity<PagingResponse<QuizAttemptResultResponse>> getMyAttemptHistory(
            @PathVariable Long quizId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(quizAttemptService.getMyAttemptHistory(quizId, page, size));
    }

    // ───────────── GET ATTEMPT ANSWERS ─────────────
    @GetMapping("/{attemptId}/answers")
    public ResponseEntity<ApiResponse<List<QuizAttemptAnswerResponse>>> getAttemptAnswers(
            @PathVariable Long attemptId
    ) {
        return ResponseEntity.ok(ApiResponse.<List<QuizAttemptAnswerResponse>>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy câu trả lời của bài làm thành công")
                .data(quizAttemptService.getAttemptAnswers(attemptId))
                .build());
    }

    // ───────────── GET ALL RESULTS BY QUIZ ─────────────
    @GetMapping("/{quizId}/results")
    public ResponseEntity<PagingResponse<QuizResultResponse>> getAllResultsByQuiz(
            @PathVariable Long quizId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String email
    ) {
        return ResponseEntity.ok(
                quizAttemptService.getAllResultsByQuiz(quizId, page, size, userName, email)
        );
    }

    @GetMapping("/history")
    public ResponseEntity<PagingResponse<QuizResult>> getAllResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(quizAttemptService.getAllResults(page, size));
    }
}