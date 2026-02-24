package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.ChangePasswordRequest;
import com.example.Auto_Grade.dto.req.UpdateAccountRequest;
import com.example.Auto_Grade.dto.req.UpdateAvatarRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.AvatarUrlResponse;
import com.example.Auto_Grade.dto.res.ProfilePersonalResponse;
import com.example.Auto_Grade.dto.res.UpdateAccountResponse;
import com.example.Auto_Grade.enums.Role;
import com.example.Auto_Grade.service.AccountService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccountById(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa tài khoản thành công")
                        .data(null)
                        .build()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<?>> updateAccountLock(
            @PathVariable Long id) {

        boolean locked = accountService.updateLockStatus(id);

        String message = locked
                ? "Khóa tài khoản thành công"
                : "Mở khóa tài khoản thành công";

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .code(HttpServletResponse.SC_OK)
                        .message(message)
                        .data(null)
                        .build()
        );
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest body) {
        accountService.changePassword(body);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Đổi mật khẩu thành công")
                        .data(null)
                        .build()
        );
    }

    @PutMapping(value = "/update-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AvatarUrlResponse>> updateAvatarUrl(
            @ModelAttribute UpdateAvatarRequest body
    ) {
        AvatarUrlResponse response = accountService.updateAvatarUrl(body);

        return ResponseEntity.ok(
                ApiResponse.<AvatarUrlResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật ảnh đại diện thành công")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfilePersonalResponse>> getProfilePersonal() {
        ProfilePersonalResponse response = accountService.getProfilePersonal();
        return ResponseEntity.ok(
                ApiResponse.<ProfilePersonalResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy thông tin cá nhân thành công")
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<UpdateAccountResponse>> updateAccountInfo(
            @Valid @RequestBody UpdateAccountRequest body) {
        UpdateAccountResponse response = accountService.updateAccount(body);
        return ResponseEntity.ok(
                ApiResponse.<UpdateAccountResponse>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Cập nhật thông tin tài khoản thành công")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách USER thành công")
                        .data(accountService.getAccountsByRole(Role.USER, page, size))
                        .build()
        );
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách TEACHER thành công")
                        .data(accountService.getAccountsByRole(Role.TEACHER, page, size))
                        .build()
        );
    }
}
