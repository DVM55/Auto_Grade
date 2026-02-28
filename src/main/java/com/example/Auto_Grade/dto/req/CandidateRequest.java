package com.example.Auto_Grade.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
@Data
public class CandidateRequest {
    @NotBlank(message = "Họ và tên thí sinh không được để trống")
    private String fullName;

    @NotBlank(message = "Phòng thi không được để trống")
    private String examRoom;

    @NotBlank(message = "Tên lớp không được để trống")
    private String className;

    private String note;
    private LocalDate dateOfBirth;
    private String gender;
}
