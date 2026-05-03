package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ContactRequest;

public interface GoogleSheetService {
    void appendContact(ContactRequest request);
}
