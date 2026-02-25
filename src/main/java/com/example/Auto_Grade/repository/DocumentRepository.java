package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
        SELECT d FROM Document d
        WHERE d.classEntity.id = :classId
        ORDER BY d.createdAt DESC
    """)
    Page<Document> findByClassId(
            @Param("classId") Long classId,
            Pageable pageable
    );



}
