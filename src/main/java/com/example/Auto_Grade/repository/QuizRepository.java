package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Quiz;
import com.example.Auto_Grade.enums.QuizAccessType;
import com.example.Auto_Grade.enums.QuizStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    boolean existsByQuizCode(String quizCode);

    @Query("""
    SELECT q FROM Quiz q
    WHERE q.creator.id = :accountId
    AND (:title IS NULL OR
        LOWER(function('unaccent', q.title)) LIKE LOWER(function('unaccent',
            CONCAT('%', CAST(:title AS string), '%')
        ))
    )
    AND (:status IS NULL OR q.status = :status)
    AND (:accessType IS NULL OR q.accessType = :accessType)
    AND (:classId IS NULL OR EXISTS (
        SELECT 1 FROM q.classes c WHERE c.id = :classId
    ))
    """)
    Page<Quiz> searchQuizzes(
            @Param("accountId") Long accountId,
            @Param("title") String title,
            @Param("status") QuizStatus status,
            @Param("accessType") QuizAccessType accessType,
            @Param("classId") Long classId,
            Pageable pageable
    );

    @Query("""
    SELECT q FROM Quiz q
    WHERE q.creator.id = :accountId
    AND (:title IS NULL OR
        LOWER(q.title) LIKE LOWER(CONCAT('%', :title, '%'))
    )
    AND (:status IS NULL OR q.status = :status)
    AND (:accessType IS NULL OR q.accessType = :accessType)
    AND (:classId IS NULL OR EXISTS (
        SELECT 1 FROM q.classes c WHERE c.id = :classId
    ))
""")
    Page<Quiz> searchWithAccent(
            @Param("accountId") Long accountId,
            @Param("title") String title,
            @Param("status") QuizStatus status,
            @Param("accessType") QuizAccessType accessType,
            @Param("classId") Long classId,
            Pageable pageable
    );

    @Query("""
    SELECT q FROM Quiz q
    JOIN q.classes c
    WHERE c.id = :classId
    AND q.status = 'PUBLISHED'
    """)
    Page<Quiz> findQuizForMember(Long classId, Pageable pageable);

    Optional<Quiz> findByQuizCode(String quizCode);
}

