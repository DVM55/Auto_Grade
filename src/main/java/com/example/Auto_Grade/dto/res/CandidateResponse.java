package com.example.Auto_Grade.dto.res;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CandidateResponse {
    private Long id;
    private String fullName;
    private String candidateNumber;
    private String examRoom;
    private String note;
    private String className;
    private LocalDate dateOfBirth;
    private String gender;
}
