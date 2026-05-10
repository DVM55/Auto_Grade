package com.example.Auto_Grade.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentExtractionService {
    String extractText(MultipartFile file);
}
