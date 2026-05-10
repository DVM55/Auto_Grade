package com.example.Auto_Grade.controller;


import com.example.Auto_Grade.dto.req.QuizRequest;
import com.example.Auto_Grade.dto.res.*;
import com.example.Auto_Grade.enums.QuizAccessType;
import com.example.Auto_Grade.enums.QuizStatus;
import com.example.Auto_Grade.service.QuizService;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> createQuiz(
            @Valid @RequestBody QuizRequest request
    ) {
        quizService.createQuiz(request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Tạo bài kiểm tra thành công")
                .data(null)
                .build());
    }

    // ───────────── DELETE ─────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Xoá quiz thành công")
                .data(null)
                .build());
    }

    @GetMapping
    public ResponseEntity<PagingResponse<QuizResponse>> getMyQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) QuizStatus status,
            @RequestParam(required = false) QuizAccessType quizAccessType
    ) {
        return ResponseEntity.ok(
                quizService.getQuiz(title, classId, status, quizAccessType, page, size)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuizDetailResponse>> getQuizDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<QuizDetailResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy chi tiết quiz thành công")
                .data(quizService.getQuizDetail(id))
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateQuiz(
            @PathVariable Long id,
            @Valid @RequestBody QuizRequest request
    ) {
        quizService.updateQuiz(id, request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(200)
                        .message("Cập nhật quiz thành công")
                        .data(null)
                        .build()
        );
    }

    // ───────────── GET QUIZZES BY CLASS (for member) ─────────────
    @GetMapping("/class/{classId}")
    public ResponseEntity<PagingResponse<QuizForMemberResponse>> getQuizByClassId(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                quizService.getQuizByClassId(classId, page, size)
        );
    }

    // ───────────── GET QUIZ DETAIL FOR MEMBER ─────────────
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<QuizDetail>> getQuizDetailForMember(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<QuizDetail>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy chi tiết quiz thành công")
                .data(quizService.getQuizDetailForMember(id))
                .build());
    }

    // ───────────── GET QUIZ BY CODE ─────────────
    @GetMapping("/code/{quizCode}")
    public ResponseEntity<ApiResponse<QuizDetail>> getQuizByCode(@PathVariable String quizCode) {
        return ResponseEntity.ok(ApiResponse.<QuizDetail>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy quiz thành công")
                .data(quizService.getQuizByCode(quizCode))
                .build());
    }

    @GetMapping("/{quizId}/statistics")
    public ResponseEntity<ApiResponse<QuizStatisticsResponse>> getQuizStatistics(
            @PathVariable Long quizId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<QuizStatisticsResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy thống kê quiz thành công")
                        .data(quizService.getQuizStatistics(quizId))
                        .build()
        );
    }

    @GetMapping("/{quizId}/question-statistics")
    public ResponseEntity<ApiResponse<List<QuestionStatisticsResponse>>> getQuestionStatistics(
            @PathVariable Long quizId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<QuestionStatisticsResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy thống kê câu hỏi thành công")
                        .data(quizService.getQuestionStatistics(quizId))
                        .build()
        );
    }

    @GetMapping("/{quizId}/export")
    public void exportResults(
            @PathVariable Long quizId,
            HttpServletResponse response
    ) {
        quizService.exportResultsToExcel(quizId, response);
    }
}

