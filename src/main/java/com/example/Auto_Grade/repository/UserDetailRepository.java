package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDetailRepository extends JpaRepository<UserDetail, Long> {
    Optional<UserDetail> findByAccountId(Long accountId);
}