package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Media;
import com.example.Auto_Grade.enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media,Long> {
    @Query("""
    SELECT m
    FROM Media m
    WHERE m.createdBy.id = :accountId
    AND (
        :fileName IS NULL OR
        LOWER(function('unaccent', m.fileName)) LIKE LOWER(function('unaccent',
            CONCAT('%', CAST(:fileName as string), '%')
        ))
    )
    AND (
        :mediaType IS NULL OR m.mediaType = :mediaType
    )
""")
    Page<Media> getMedias(
            @Param("accountId") Long accountId,
            @Param("fileName") String fileName,
            @Param("mediaType") MediaType mediaType,
            Pageable pageable
    );


    List<Media> findAllByCreatedById(Long creatorId);

    @Modifying
    @Query("""
    DELETE FROM Media m
    WHERE m.id IN :ids
    AND m.createdBy.id = :accountId
""")
    void deleteByIdsAndAccountId(
            @Param("ids") List<Long> ids,
            @Param("accountId") Long accountId
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM Media m WHERE m.mediaType = :mediaType AND m.createdBy.id = :accountId")
    void deleteByMediaTypeAndAccountId(@Param("mediaType") MediaType mediaType,
                                       @Param("accountId") Long accountId);
}
