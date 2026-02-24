package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.*;
import com.example.Auto_Grade.dto.res.TokenResponse;

public interface AuthService {
    void register(RegisterRequest request);
    void login(LoginRequest request);
    void ForgotPassword(EmailRequest request);
    boolean verifyOtp(VerifyAccountRequest request);
    void resetPassword(ResetPasswordRequest request);
    TokenResponse refreshToken(RefreshTokenRequest request);
    void sendOTP( EmailRequest request);
    TokenResponse verifyAccount(VerifyAccountRequest request);
    void logout(String authHeader);
}
