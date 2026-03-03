package com.example.Auto_Grade.enums;
import lombok.Getter;

@Getter
public enum GradeType {

    TOAN("Toán"),

    LY_HOA_SINH_DIA("Vật lý, Hóa học, Sinh học, Địa lý"),

    SU_GDKTPL_TIN_CONG_NGHE("Lịch sử, GDKT&PL, Tin học, Công nghệ"),

    NGOAI_NGU("Ngoại ngữ");

    private final String displayName;

    GradeType(String displayName) {
        this.displayName = displayName;
    }

}
