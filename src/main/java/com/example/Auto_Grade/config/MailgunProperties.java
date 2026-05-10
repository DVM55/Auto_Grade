package com.example.Auto_Grade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "mailgun")
public class MailgunProperties {
    private String apiKey;
    private String domain;
    private String from;
    private String fromName;
    private String baseUrl;
}
