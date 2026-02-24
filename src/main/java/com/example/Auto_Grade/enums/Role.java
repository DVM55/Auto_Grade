package com.example.Auto_Grade.enums;

public enum Role {
    USER,
    ADMIN,
    TEACHER;

    public static boolean isValidRole(String value) {
        for (Role role : values()) {
            if (role.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}