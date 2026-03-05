package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MediaRepository extends JpaRepository<Media,Long> {
    @Query("""
    SELECT m
    FROM Media m
    WHERE m.createdBy.id = :accountId
    AND LOWER(function('unaccent', m.fileName))
        LIKE LOWER(function('unaccent',
            CONCAT('%', CAST(COALESCE(:fileName, '') as string), '%')
        ))
    """)
    Page<Media> getMedias(
            @Param("accountId") Long accountId,
            @Param("fileName") String fileName,
            Pageable pageable
    );
}
