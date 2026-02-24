package com.example.Auto_Grade.service;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final RedisService redisService;
    private final MailService mailService;

    public String generateOtp(String email, int ttlSeconds) {
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        redisService.setWithTTL("OTP:" + email, otp, ttlSeconds, TimeUnit.SECONDS);
        return otp;
    }

    // Kiểm tra OTP
    public boolean verifyOtp(String email, String otp) {
        String key = "OTP:" + email;
        Object cached = redisService.get(key);

        return cached != null && cached.toString().equals(otp);
    }

    // Xóa OTP sau khi reset thành công
    public void deleteOtp(String email) {
        redisService.delete("OTP:" + email);
    }

    @Async
    public void sendOTPAsync(String email, int type) {
        int ttl;
        String subject;
        String body;

        switch (type) {
            case 1 -> {
                ttl = 60;
                subject = "Mã OTP xác thực tài khoản";
                body = "<p>Xin chào,</p>" +
                        "<p>Mã OTP của bạn là: <b>%s</b></p>" +
                        "<p>Mã này có hiệu lực trong 1 phút.</p>";
            }
            case 2 -> {
                ttl = 900;
                subject = "Mã OTP khôi phục mật khẩu";
                body = "<p>Xin chào,</p>" +
                        "<p>Mã OTP của bạn là: <b>%s</b></p>" +
                        "<p>Mã này có hiệu lực trong 15 phút.</p>";
            }
            default -> throw new IllegalArgumentException("Loại OTP không hợp lệ: " + type);
        }

        String otp = generateOtp(email, ttl);
        mailService.sendHtml(email, subject, String.format(body, otp));
    }

}
