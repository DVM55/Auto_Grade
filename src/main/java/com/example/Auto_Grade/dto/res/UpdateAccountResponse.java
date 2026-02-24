package com.example.Auto_Grade.dto.res;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAccountResponse {
    private Long id;
    private String email;
    private String username;
    private String phone;
    private String gender;
    private LocalDate date_of_birth;
    private String address;
}