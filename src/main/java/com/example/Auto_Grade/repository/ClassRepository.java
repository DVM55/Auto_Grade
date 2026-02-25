package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Class;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClassRepository extends JpaRepository<Class, Long> {
    boolean existsByClassCode(String classCode);

    Optional<Class> findByClassCode(String classCode);

    @Query("""
    SELECT DISTINCT c
    FROM Class c
    LEFT JOIN ClassMember cm
        ON cm.classEntity.id = c.id
        AND cm.account.id = :userId
        AND cm.status = 'APPROVED'
    WHERE
        (c.creator.id = :userId OR cm.id IS NOT NULL)
        AND LOWER(function('unaccent', c.title))
            LIKE LOWER(function('unaccent',
                CONCAT('%', CAST(COALESCE(:title, '') as string), '%')
            ))
        AND LOWER(function('unaccent', c.classCode))
            LIKE LOWER(function('unaccent',
                CONCAT('%', CAST(COALESCE(:classCode, '') as string), '%')
            ))
""")
    Page<Class> getClasses(
            @Param("userId") Long userId,
            @Param("title") String title,
            @Param("classCode") String classCode,
            Pageable pageable
    );

    @Query("""
    SELECT COUNT(c) > 0
    FROM Class c
    LEFT JOIN ClassMember cm
        ON cm.classEntity.id = c.id
        AND cm.account.id = :userId
        AND cm.status = 'APPROVED'
    WHERE c.id = :classId
    AND (
        c.creator.id = :userId
        OR cm.id IS NOT NULL
    )
    """)
    boolean hasAccessToClass(
            @Param("classId") Long classId,
            @Param("userId") Long userId
    );
}
