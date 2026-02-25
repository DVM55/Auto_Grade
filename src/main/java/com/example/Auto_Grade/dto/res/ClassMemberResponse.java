package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.entity.ClassMember;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassMemberResponse {
    private Long id;
    private Long accountId;
    private String email;
    private String username;
    private String avatarUrl;
}
