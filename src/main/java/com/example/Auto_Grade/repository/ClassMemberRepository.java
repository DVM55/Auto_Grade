package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.ClassMember;
import com.example.Auto_Grade.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {
    long countByClassEntityIdAndStatus(Long classId, MemberStatus status);

    boolean existsByClassEntityIdAndAccountId(Long classId, Long accountId);

    @Query("""
    SELECT cm
    FROM ClassMember cm
    WHERE cm.classEntity.id = :classId
    AND cm.status = :status
    AND LOWER(function('unaccent', cm.account.username))
        LIKE LOWER(function('unaccent',
            CONCAT('%', CAST(COALESCE(:username, '') as string), '%')
        ))
    AND LOWER(function('unaccent', cm.account.email))
        LIKE LOWER(function('unaccent',
            CONCAT('%', CAST(COALESCE(:email, '') as string), '%')
        ))
""")
    Page<ClassMember> findMembersByStatusAndFilters(
            @Param("classId") Long classId,
            @Param("status") MemberStatus status,
            @Param("username") String username,
            @Param("email") String email,
            Pageable pageable
    );

    boolean existsByClassEntity_IdAndAccount_IdAndStatus(
            Long classId,
            Long accountId,
            MemberStatus status
    );

    @Query("""
    SELECT cm.status
    FROM ClassMember cm
    WHERE cm.classEntity.id = :classId
    AND cm.account.id = :userId
""")
    Optional<MemberStatus> findStatusByClassAndUser(
            @Param("classId") Long classId,
            @Param("userId") Long userId
    );
}
