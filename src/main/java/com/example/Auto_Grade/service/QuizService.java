package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.QuizRequest;
import com.example.Auto_Grade.dto.res.*;
import com.example.Auto_Grade.enums.QuizAccessType;
import com.example.Auto_Grade.enums.QuizStatus;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;


public interface QuizService {
    void createQuiz(QuizRequest request);
    void deleteQuiz(Long quizId);
    PagingResponse<QuizResponse> getQuiz(String title, Long classId, QuizStatus status, QuizAccessType quizAccessType, int page, int size);
    QuizDetailResponse getQuizDetail(Long quizId);

    PagingResponse<QuizForMemberResponse> getQuizByClassId(Long classId, int page, int size);

    void updateQuiz(Long quizId, QuizRequest request);

    QuizDetail getQuizDetailForMember(Long quizId);

    QuizDetail getQuizByCode(String quizCode);

    QuizStatisticsResponse getQuizStatistics(Long quizId);

    List<QuestionStatisticsResponse> getQuestionStatistics(Long quizId);

    void exportResultsToExcel(Long quizId, HttpServletResponse response);
}

