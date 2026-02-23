package com.example.Auto_Grade.service.implementation;


import com.example.Auto_Grade.service.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.mail.SimpleMailMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendSimple(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject != null ? subject : "(No Subject)");
            msg.setText(body != null ? body : "");
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("❌ Failed to send simple email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8"
            );

            helper.setTo(to);
            helper.setSubject(subject != null ? subject : "(No Subject)");
            helper.setText(htmlBody != null ? htmlBody : "", true);

            mailSender.send(message);

        } catch (Exception e) {
            log.error("❌ Failed to send HTML email to {}: {}", to, e.getMessage(), e);
        }
    }
}