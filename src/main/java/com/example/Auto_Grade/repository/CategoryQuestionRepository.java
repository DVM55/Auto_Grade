package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.CategoryQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryQuestionRepository extends JpaRepository<CategoryQuestion, Long> {
    List<CategoryQuestion> findAllByCreatorId(Long creatorId);
}

