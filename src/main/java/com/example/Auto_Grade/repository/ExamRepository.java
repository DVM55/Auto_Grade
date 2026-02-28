package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    boolean existsByName(String name);

    @Query("""
    SELECT e
    FROM Exam e
    WHERE e.creator.id = :userId
        AND (
            :name IS NULL
            OR LOWER(function('unaccent', e.name))
                LIKE LOWER(function('unaccent',
                    CAST(CONCAT('%', :name, '%') as string)
                ))
        )
    """)
    Page<Exam> getExams(
            @Param("userId") Long userId,
            @Param("name") String name,
            Pageable pageable
    );
}
