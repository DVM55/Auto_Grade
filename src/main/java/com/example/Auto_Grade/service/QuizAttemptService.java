package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.AnswerRequest;
import com.example.Auto_Grade.dto.req.SaveAnswerRequest;
import com.example.Auto_Grade.dto.res.*;

import java.util.List;

public interface QuizAttemptService {
    QuizAttemptResultResponse submitQuiz(Long attemptId, List<SaveAnswerRequest> request);
    StartQuizResponse startQuiz(Long quizId);
    PagingResponse<QuizAttemptResultResponse> getMyAttemptHistory(Long quizId, int page, int size);
    List<QuizAttemptAnswerResponse> getAttemptAnswers(Long attemptId);
    PagingResponse<QuizResultResponse> getAllResultsByQuiz(Long quizId, int page, int size, String userName, String email);
    void saveAnswer(AnswerRequest request);
    PagingResponse<QuizResult> getAllResults(int page, int size);
}
