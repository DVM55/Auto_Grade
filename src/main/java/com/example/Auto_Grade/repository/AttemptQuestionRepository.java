package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.AttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, Long> {
    List<AttemptQuestion> findByAttemptIdOrderByCreatedAt(Long attemptId);
}
