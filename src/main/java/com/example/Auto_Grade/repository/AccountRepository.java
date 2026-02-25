package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;
import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Page<Account> findByRole(Role role, Pageable pageable);

    @Query(value = """
    SELECT 
        a.id AS id,
        a.email AS email,
        a.username AS username,
        a.object_key AS "object_key",
        ud.phone AS phone,
        ud.date_of_birth AS date_of_birth,
        ud.address AS address,
        ud.gender AS gender 
    FROM accounts a
    LEFT JOIN user_details ud 
        ON ud.user_id = a.id
    WHERE a.id = :accountId
    """, nativeQuery = true)
    Optional<Map<String, Object>> findAccountDetail(@Param("accountId") Long id);
}