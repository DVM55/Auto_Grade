package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ClassCodeRequest;
import com.example.Auto_Grade.dto.res.ClassMemberResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;

public interface ClassMemberService {
    void joinClass(ClassCodeRequest request);

    void approveMember(Long id);

    void removeMember(Long id);

    PagingResponse<ClassMemberResponse> getApprovedMembers(
            Long classId,
            String username,
            String email,
            int page,
            int size
    );

    PagingResponse<ClassMemberResponse> getPendingMembers(
            Long classId,
            String username,
            String email,
            int page,
            int size
    );
}
