package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountResponse {

    private Long id;
    private String email;
    private String username;
    private String avatarUrl;
    private Role role;
    private boolean locked;
}
