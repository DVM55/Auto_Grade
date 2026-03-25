package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.CategoryQuestion;
import com.example.Auto_Grade.entity.GroupQuestion;
import com.example.Auto_Grade.entity.Question;

import com.example.Auto_Grade.enums.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findAllByCreatorId(Long creatorId);

    @Query("""
    SELECT q FROM Question q
    WHERE q.creator.id = :creatorId
    AND (
        :content IS NULL OR
        LOWER(function('unaccent', q.content)) LIKE LOWER(function('unaccent',
            CONCAT('%', CAST(:content as string), '%')
        ))
    )
    AND (
        :questionFilterMode = 'ALL'
        OR (
            :questionFilterMode = 'UNCLASSIFIED'
            AND (
                q.categoryQuestion IS NULL
                OR q.groupQuestion IS NULL
            )
        )
    )
    AND (:categoryId IS NULL OR q.categoryQuestion.id = :categoryId)
    AND (:groupId IS NULL OR q.groupQuestion.id = :groupId)
    AND (:questionType IS NULL OR q.questionType = :questionType)
""")
    Page<Question> searchQuestions(
            @Param("creatorId") Long creatorId,
            @Param("content") String content,
            @Param("categoryId") Long categoryId,
            @Param("groupId") Long groupId,
            @Param("questionType") QuestionType questionType,
            @Param("questionFilterMode") String questionFilterMode,
            Pageable pageable
    );

    @Query("""
    SELECT q FROM Question q
    WHERE q.creator.id = :creatorId
    AND (
        :content IS NULL OR
        LOWER(q.content) LIKE LOWER(CONCAT('%', :content, '%'))
    )
    AND (
        :questionFilterMode = 'ALL'
        OR (
            :questionFilterMode = 'UNCLASSIFIED'
            AND (
                q.categoryQuestion IS NULL
                OR q.groupQuestion IS NULL
            )
        )
    )
    AND (:categoryId IS NULL OR q.categoryQuestion.id = :categoryId)
    AND (:groupId IS NULL OR q.groupQuestion.id = :groupId)
    AND (:questionType IS NULL OR q.questionType = :questionType)
""")
    Page<Question> searchWithAccent(
            @Param("creatorId") Long creatorId,
            @Param("content") String content,
            @Param("categoryId") Long categoryId,
            @Param("groupId") Long groupId,
            @Param("questionType") QuestionType questionType,
            @Param("questionFilterMode") String questionFilterMode,
            Pageable pageable
    );

    @Modifying
    @Query("""
    DELETE FROM Question q
    WHERE q.id IN :ids
    AND q.creator.id = :creatorId
""")
    void deleteQuestionByIdsAndCreatorId(
            @Param("ids") List<Long> ids,
            @Param("creatorId") Long creatorId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Question q
    SET q.categoryQuestion = :category,
        q.groupQuestion = :group
    WHERE q.id IN :ids
      AND q.creator.id = :creatorId
""")
    void updateQuestionByIdsAndCreatorId(
            @Param("ids") List<Long> ids,
            @Param("category") CategoryQuestion category,
            @Param("group") GroupQuestion group,
            @Param("creatorId") Long creatorId
    );
}

