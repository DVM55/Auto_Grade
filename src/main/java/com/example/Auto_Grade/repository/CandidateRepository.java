package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findAllByExam_Id(Long examId);
    void deleteAllByExam_Id(Long examId);
    List<Candidate> findAllByExam_IdOrderByIdAsc(Long examId);

    @Query("""
    SELECT c
    FROM Candidate c
    WHERE c.exam.id = :examId

    AND LOWER(function('unaccent', COALESCE(c.fullName, '')))
        LIKE LOWER(function('unaccent',
            CONCAT('%', COALESCE(:fullName, ''), '%')
        ))

    AND LOWER(function('unaccent', COALESCE(c.candidateNumber, '')))
        LIKE LOWER(function('unaccent',
            CONCAT('%', COALESCE(:candidateNumber, ''), '%')
        ))

    AND LOWER(function('unaccent', COALESCE(c.examRoom, '')))
        LIKE LOWER(function('unaccent',
            CONCAT('%', COALESCE(:examRoom, ''), '%')
        ))

    AND LOWER(function('unaccent', COALESCE(c.note, '')))
        LIKE LOWER(function('unaccent',
            CONCAT('%', COALESCE(:note, ''), '%')
        ))

    AND LOWER(function('unaccent', COALESCE(c.className, '')))
        LIKE LOWER(function('unaccent',
            CONCAT('%', COALESCE(:className, ''), '%')
        ))
""")
    Page<Candidate> findByExamIdWithFilters(
            @Param("examId") Long examId,
            @Param("fullName") String fullName,
            @Param("candidateNumber") String candidateNumber,
            @Param("examRoom") String examRoom,
            @Param("note") String note,
            @Param("className") String className,
            Pageable pageable
    );
}
