package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.*;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.TokenResponse;
import com.example.Auto_Grade.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest registerRequest) {
         authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_CREATED)
                        .message("Đăng ký tài khoản thành công!")
                        .data(null)
                        .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody @Valid LoginRequest request) {
        authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Vui lòng xác thực tài khoản bước 2")
                        .data(null)
                        .build()
        );
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOTP(
            @RequestBody @Valid EmailRequest request
    ){
        authService.sendOTP(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Đã gửi mã OTP đến email của bạn")
                        .data(null)
                        .build()
        );
    }

    @PostMapping("/verify-account")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyAccount(
            @RequestBody @Valid VerifyAccountRequest request
    ){
        TokenResponse data = authService.verifyAccount(request);
        return ResponseEntity.ok(
                ApiResponse.<TokenResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Đăng nhập thành công")
                        .data(data)
                        .build()
        );
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String token
    ) {
        authService.logout(token);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Đăng xuất thành công")
                        .data(null)
                        .build()
        );
    }

    @PostMapping("/refresh-accessToken")
    public ResponseEntity<ApiResponse<TokenResponse>>refreshToken(@RequestBody RefreshTokenRequest request) {

        TokenResponse token = authService.refreshToken(request);
        return ResponseEntity.ok(
                ApiResponse.<TokenResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Làm mới token thành công")
                        .data(token)
                        .build()
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid EmailRequest request){
        authService.ForgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Đã gửi mã OTP đến email của bạn")
                        .data(null)
                        .build()
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<?>> verifyOtp(@RequestBody VerifyAccountRequest request) {

        authService.verifyOtp(request);
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xác thực OTP thành công")
                        .data(null)
                        .build()
        );
    }

    @PostMapping("/reset-password")
    public  ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request){
        authService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Đặt lại mật khẩu thành công")
                        .data(null)
                        .build()
        );
    }


}
