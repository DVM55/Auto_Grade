package com.example.Auto_Grade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ChatBotConfig {

    @Bean
    public RestClient chatBotRestClient(
            RestClient.Builder builder,
            @Value("${chatbot.agent.base-url}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
