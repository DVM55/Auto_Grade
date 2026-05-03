package com.example.Auto_Grade.service.implementation;


import com.example.Auto_Grade.dto.req.ContactRequest;
import com.example.Auto_Grade.service.GoogleSheetService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleSheetServiceImpl implements GoogleSheetService {

    @Value("${google.sheet.id}")
    private String spreadsheetId;

    @Value("${google.sheet.range}")
    private String range;

    @Override
    public void appendContact(ContactRequest request) {
        try {
            InputStream is = new ClassPathResource("credentials.json").getInputStream();

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(is)
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/spreadsheets"));

            Sheets service = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            ).setApplicationName("Auto Grade").build();

            List<Object> row = Arrays.asList(
                    request.getFullname(),
                    request.getEmail(),
                    request.getMessage(),
                    LocalDateTime.now().toString(),
                    LocalDate.now().toString()
            );

            ValueRange body = new ValueRange()
                    .setValues(Collections.singletonList(row));

            service.spreadsheets().values()
                    .append(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute();

        } catch (Exception e) {
            throw new RuntimeException("Ghi Google Sheet lỗi: " + e.getMessage());
        }
    }
}