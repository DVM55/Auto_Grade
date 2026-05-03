package com.example.Auto_Grade.utils;

import com.example.Auto_Grade.entity.Quiz;
import com.example.Auto_Grade.entity.QuizAnswer;
import com.example.Auto_Grade.entity.QuizAttempt;
import com.example.Auto_Grade.enums.AttemptStatus;
import com.example.Auto_Grade.repository.QuizAnswerRepository;
import com.example.Auto_Grade.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AttemptHelper {

    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireAttempt(QuizAttempt attempt) {
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(attempt.getExpiredAt());

        // Tính điểm
        Quiz quiz = attempt.getQuiz();
        List<QuizAnswer> allAnswers = answerRepository.findByAttemptId(attempt.getId());
        int totalQuestions = quiz.getQuestions().size();

        int correctCount = (int) allAnswers.stream()
                .filter(ans -> Boolean.TRUE.equals(ans.getIsCorrect()))
                .count();

        double totalScore;
        if (Boolean.TRUE.equals(quiz.getAutoScore())) {
            double scorePerQuestion = (quiz.getTotalScore() != null ? quiz.getTotalScore() : 0.0)
                    / totalQuestions;
            totalScore = Math.round(scorePerQuestion * correctCount * 100.0) / 100.0;
        } else {
            totalScore = allAnswers.stream()
                    .filter(ans -> Boolean.TRUE.equals(ans.getIsCorrect()))
                    .mapToDouble(ans -> ans.getQuestion().getScore() != null
                            ? ans.getQuestion().getScore() : 0.0)
                    .sum();
        }

        attempt.setCorrectCount(correctCount);
        attempt.setTotalScore(totalScore);

        attemptRepository.save(attempt);
    }
}
