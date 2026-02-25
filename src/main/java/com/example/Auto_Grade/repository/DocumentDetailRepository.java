package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.DocumentDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentDetailRepository extends JpaRepository<DocumentDetail, Long> {
    Page<DocumentDetail> findByDocument_Id(Long documentId, Pageable pageable);
}
