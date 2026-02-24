package com.example.Auto_Grade.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilePersonalResponse {
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String phone;
    private LocalDate date_of_birth;
    private String address;
    private String gender;
}
