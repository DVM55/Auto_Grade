package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.ContactRequest;
import com.example.Auto_Grade.service.ContactService;

import com.example.Auto_Grade.service.GoogleSheetService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private  final GoogleSheetService googleSheetService;

    @Override
    public void handleContact(ContactRequest request) {
        // Lưu thông tin liên hệ vào Google Sheets
        googleSheetService.appendContact(request);
    }
}