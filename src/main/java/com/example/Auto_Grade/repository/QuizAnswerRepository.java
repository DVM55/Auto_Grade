package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.QuizAnswer;
import com.example.Auto_Grade.enums.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findByAttemptId(Long attemptId);

    Optional<QuizAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    // Lấy toàn bộ answers (kèm selectedOptions) của một quiz
    @Query("""
    SELECT DISTINCT a FROM QuizAnswer a
    JOIN FETCH a.question q
    LEFT JOIN FETCH a.selectedOptions
    WHERE a.attempt.quiz.id = :quizId
      AND a.attempt.status = :status
""")
    List<QuizAnswer> findAllAnswersByQuizIdAndStatus(
            @Param("quizId") Long quizId,
            @Param("status") AttemptStatus status
    );
}
