package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ClassCodeRequest;
import com.example.Auto_Grade.dto.res.ClassMemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClassMemberService {
    void joinClass(ClassCodeRequest request);

    void approveMember(Long id);

    void removeMember(Long id);

    Page<ClassMemberResponse> getApprovedMembers(
            Long classId,
            String username,
            String email,
            Pageable pageable
    );

    Page<ClassMemberResponse> getPendingMembers(
            Long classId,
            String username,
            String email,
            Pageable pageable
    );
}
