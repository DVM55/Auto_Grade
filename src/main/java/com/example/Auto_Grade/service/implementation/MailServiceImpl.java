package com.example.Auto_Grade.service.implementation;


import com.example.Auto_Grade.config.MailgunProperties;
import com.example.Auto_Grade.service.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final MailgunProperties props;
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public void sendSimple(String to, String subject, String body) {
        sendEmail(to, subject, null, body);
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        sendEmail(to, subject, htmlBody, null);
    }

    private void sendEmail(String to, String subject, String htmlBody, String textBody) {
        try {
            String fromField = props.getFromName() + " <" + props.getFrom() + ">";

            FormBody.Builder formBuilder = new FormBody.Builder()
                    .add("from", fromField)
                    .add("to", to)
                    .add("subject", subject != null ? subject : "(No Subject)");

            if (htmlBody != null) {
                formBuilder.add("html", htmlBody);
            } else {
                formBuilder.add("text", textBody != null ? textBody : "");
            }

            String url = props.getBaseUrl() + "/" + props.getDomain() + "/messages";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization",
                            okhttp3.Credentials.basic("api", props.getApiKey()))
                    .post(formBuilder.build())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("✅ Email sent to {} | status: {}", to, response.code());
                } else {
                    String body2 = response.body() != null ? response.body().string() : "null";
                    log.error("❌ Mailgun error | status: {} | body: {}", response.code(), body2);
                }
            }

        } catch (IOException e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }
}