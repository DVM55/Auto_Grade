package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.ContactRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.service.ContactService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    // ================= SEND CONTACT =================
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendContact(
            @Valid @RequestBody ContactRequest request) {

        contactService.handleContact(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Gửi message thành công")
                        .data(null)
                        .build()
        );
    }
}