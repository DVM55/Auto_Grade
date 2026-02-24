package com.example.Auto_Grade.service;

public interface JwtService {
    String generateAccessToken(Long userId, String username, String role);

    String generateRefreshToken(Long userId, String username, String role);

    void validateAccessToken(String token);

    void  validateRefreshToken(String token);

    Long getAccountIdFromAccessToken(String token);

    Long getAccountIdFromRefreshToken(String token);

    String getUsernameFromAccessToken(String token);

    String getRoleFromAccessToken(String token);

    Long getExpirationTime(String token);

    Long extractUserIdIgnoreExpiration(String token);

    void blacklistToken(String token);

    boolean isTokenBlacklisted(String token);
}
