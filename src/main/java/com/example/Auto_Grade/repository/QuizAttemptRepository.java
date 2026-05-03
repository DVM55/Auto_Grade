package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.QuizAttempt;
import com.example.Auto_Grade.enums.AttemptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    long countByQuizIdAndCreator_Id(Long quizId, Long creatorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM QuizAttempt a WHERE a.id = :id")
    Optional<QuizAttempt> findByIdWithLock(@Param("id") Long id);

    Optional<QuizAttempt> findByQuizIdAndCreator_IdAndStatus(
            Long quizId, Long studentId, AttemptStatus status);

    long countByQuizIdAndStatus(Long quizId, AttemptStatus status);

    Page<QuizAttempt> findByQuizIdAndCreator_IdAndStatus(
            Long quizId, Long creatorId, AttemptStatus status, Pageable pageable
    );

    @Query("""
        SELECT DISTINCT qa.quiz.id
        FROM QuizAttempt qa
        WHERE qa.creator.id = :accountId
          AND qa.quiz.id IN :quizIds
          AND qa.status = :status
    """)
    List<Long> findSubmittedQuizIds(Long accountId, List<Long> quizIds, AttemptStatus status);

    List<QuizAttempt> findByQuizIdAndStatus(Long quizId, AttemptStatus status);

    @Query("""
    SELECT a FROM QuizAttempt a
    JOIN Account c ON c.id = a.creator.id
    WHERE a.quiz.id = :quizId
      AND a.status = :status
      AND (
            LOWER(c.username) LIKE LOWER('%' || :username || '%')
         OR LOWER(c.email) LIKE LOWER('%' || :email || '%')
      )
""")
    Page<QuizAttempt> searchByUsernameAndEmail(
            @Param("quizId") Long quizId,
            @Param("status") AttemptStatus status,
            @Param("username") String username,
            @Param("email") String email,
            Pageable pageable
    );

    @Query("""
    SELECT a FROM QuizAttempt a
    WHERE a.quiz.id = :quizId
      AND a.status = :status
      AND (
            :username IS NULL OR
            LOWER(function('unaccent', a.creator.username)) LIKE LOWER(function('unaccent',
                CONCAT('%', CAST(:username as string), '%')
            ))
      )
      AND (
            :email IS NULL OR
            LOWER(function('unaccent', a.creator.email)) LIKE LOWER(function('unaccent',
                CONCAT('%', CAST(:email as string), '%')
            ))
      )
""")
    Page<QuizAttempt> searchWithoutAccent(
            @Param("quizId") Long quizId,
            @Param("status") AttemptStatus status,
            @Param("username") String username,
            @Param("email") String email,
            Pageable pageable
    );

    Page<QuizAttempt> findByCreator_IdAndStatus(Long creatorId, AttemptStatus status, Pageable pageable);
}
