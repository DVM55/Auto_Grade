package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.QuizQuestionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuizQuestionConfigRepository extends JpaRepository<QuizQuestionConfig, Long> {
    List<QuizQuestionConfig> findByQuizId(Long quizId);

    @Transactional
    void deleteByQuizId(Long quizId);
}
