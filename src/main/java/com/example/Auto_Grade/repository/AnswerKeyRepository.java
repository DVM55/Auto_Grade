package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.AnswerKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface AnswerKeyRepository extends JpaRepository<AnswerKey, Long> {
    @Query("""
       SELECT a.paperCode
       FROM AnswerKey a
       WHERE a.examSession.id = :examSessionId
       """)
    Set<String> findPaperCodesByExamId(Long examSessionId);

    void deleteByExamSession_Id(Long examSessionId);

    @Query("""
       SELECT ak FROM AnswerKey ak
       LEFT JOIN FETCH ak.details
       WHERE ak.examSession.id = :examSessionId
       """)
    List<AnswerKey> findAllWithDetailsByExamSession_Id(Long examSessionId);

    boolean existsByExamSession_IdAndPaperCodeAndIdNot(
            Long examSessionId,
            String paperCode,
            Long id
    );
}
