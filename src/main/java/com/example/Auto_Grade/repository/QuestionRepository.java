package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findAllByCreatorId(Long creatorId);

    @Query("""
        SELECT q FROM Question q
        WHERE q.creator.id = :creatorId
        AND (:categoryId IS NULL OR q.categoryQuestion.id = :categoryId)
        AND (:groupId IS NULL OR q.groupQuestion.id = :groupId)
    """)
    Page<Question> searchQuestionBank(
            @Param("creatorId") Long creatorId,
            @Param("categoryId") Long categoryId,
            @Param("groupId") Long groupId,
            Pageable pageable
    );
}

