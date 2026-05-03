package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.GroupQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupQuestionRepository extends JpaRepository<GroupQuestion, Long> {
    @Query("""
    SELECT g FROM GroupQuestion g
    WHERE g.creator.id = :accountId
    AND (
        :name IS NULL OR
        LOWER(function('unaccent', g.name)) LIKE LOWER(function('unaccent',
            CONCAT('%', CAST(:name as string), '%')
        ))
    )
""")
    Page<GroupQuestion> findAllByCreatorId(
            @Param("accountId") Long accountId,
            @Param("name") String name,
            Pageable pageable
    );

    @Query("""
    SELECT g FROM GroupQuestion g
    WHERE g.creator.id = :accountId
    AND (
        :name IS NULL OR
        LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%'))
    )
""")
    Page<GroupQuestion> searchWithAccent(
            @Param("accountId") Long accountId,
            @Param("name") String name,
            Pageable pageable
    );
}

