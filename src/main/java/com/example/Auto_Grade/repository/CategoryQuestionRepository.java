package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.CategoryQuestion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface CategoryQuestionRepository extends JpaRepository<CategoryQuestion, Long> {
    @Query("""
    SELECT c FROM CategoryQuestion c
    WHERE c.creator.id = :accountId
    AND (
        :name IS NULL OR
        LOWER(function('unaccent', c.name)) LIKE LOWER(function('unaccent',
            CONCAT('%', CAST(:name as string), '%')
        ))
    )
""")
    Page<CategoryQuestion> findAllByCreatorId(
            @Param("accountId") Long accountId,
            @Param("name") String name,
            Pageable pageable
    );

    @Query("""
    SELECT c FROM CategoryQuestion c
    WHERE c.creator.id = :accountId
    AND (
        :name IS NULL OR
        LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
    )
""")
    Page<CategoryQuestion> searchWithAccent(
            @Param("accountId") Long accountId,
            @Param("name") String name,
            Pageable pageable
    );
}

