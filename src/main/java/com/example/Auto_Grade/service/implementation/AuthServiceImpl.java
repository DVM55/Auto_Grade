package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.*;
import com.example.Auto_Grade.dto.res.TokenResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.Key;
import com.example.Auto_Grade.enums.Role;
import com.example.Auto_Grade.exception.ConflictException;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.KeyRepository;
import com.example.Auto_Grade.service.AuthService;
import com.example.Auto_Grade.service.JwtService;
import com.example.Auto_Grade.service.OtpService;
import com.example.Auto_Grade.service.RedisService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final KeyRepository keyRepository;
    private final OtpService otpService;
    private final RedisService redisService;

    @Override
    public void register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã được sử dụng");
        }

        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username đã được sử dụng");
        }

        if (!Role.isValidRole(request.getRole())) {
            throw new IllegalArgumentException("Role không hợp lệ");
        }

        Account account = Account.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.valueOf(request.getRole()))
                .build();

        accountRepository.save(account);
    }

    @Override
    public void login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        //System.out.println("MAIN THREAD = " + Thread.currentThread().getName());
        otpService.sendOTPAsync(request.getEmail(), 1);
    }

    @Override
    public TokenResponse verifyAccount(VerifyAccountRequest request) {
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!valid) {
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new EntityNotFoundException("Tài khoản không tồn tại với email:" + request.getEmail()));

        String accessToken = jwtService.generateAccessToken(
                account.getId(),
                account.getUsername(),
                account.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(
                account.getId(),
                account.getUsername(),
                account.getRole().name()
        );

        System.out.println("AccessToken:" + accessToken);
        System.out.println("RefreshToken:" + refreshToken);

        Optional<Key> keyOptional = keyRepository.findByAccount_Id(account.getId());

        Key key = keyOptional.orElseGet(Key::new);

        key.setAccount(account);
        key.setRefreshToken(refreshToken);

        keyRepository.save(key);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void ForgotPassword(EmailRequest request) {
        if (!accountRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email này chưa được đăng ký. Không thể thực hiện chức năng này");
        }
        otpService.sendOTPAsync(request.getEmail(), 2);
    }

    @Override
    public boolean verifyOtp(VerifyAccountRequest request) {
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!valid) {
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }
        return true;
    }

    @Override
    public void sendOTP(EmailRequest request) {
        switch (request.getType()) {
            case 1:
                otpService.sendOTPAsync(request.getEmail(), 1);
                break;

            case 2:
                otpService.sendOTPAsync(request.getEmail(), 2);
                break;

            default:
                throw new IllegalArgumentException("Loại OTP không hợp lệ: " + request.getType());
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!valid) {
            throw new IllegalArgumentException("Phiên đã hết hạn, vui lòng thực hiện quên mật khẩu lại!");
        }

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với email: " + request.getEmail()));

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        // ✅ Xoá OTP sau khi reset thành công
        otpService.deleteOtp(request.getEmail());
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        keyRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new EntityNotFoundException("Refresh token không hợp lệ"));
        jwtService.validateRefreshToken(request.getRefreshToken());
        Long userId = jwtService.getAccountIdFromRefreshToken(request.getRefreshToken());
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + userId));
        String newAccessToken = jwtService.generateAccessToken(
                account.getId(),
                account.getUsername(),
                account.getRole().name()
        );
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .build();
    }

    @Override
    public void logout(String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException(
                    "Token Invalid!"
            );
        }

        String token = authHeader.substring(7);
        jwtService.blacklistToken(token);
    }
}
