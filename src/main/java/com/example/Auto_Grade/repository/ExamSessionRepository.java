package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.ExamSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {
    boolean existsByExamIdAndSessionName(Long examId, String sessionName);

    @Query("""
    SELECT es
    FROM ExamSession es
    WHERE es.exam.id = :examId
        AND (
            :sessionName IS NULL
            OR LOWER(function('unaccent', es.sessionName))
                LIKE LOWER(function('unaccent',
                    CAST(CONCAT('%', :sessionName, '%') as string)
                ))
        )
    """)
    Page<ExamSession> getExamSessions(
            @Param("examId") Long examId,
            @Param("sessionName") String sessionName,
            Pageable pageable
    );

    @Query("""
       SELECT CASE WHEN COUNT(es) > 0 THEN true ELSE false END
       FROM ExamSession es
       WHERE es.id = :examSessionId
       AND es.exam.creator.id = :accountId
       """)
    boolean hasImportPermission(Long examSessionId, Long accountId);
}
