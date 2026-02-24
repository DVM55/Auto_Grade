package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}