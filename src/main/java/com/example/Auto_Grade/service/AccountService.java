package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ChangePasswordRequest;
import com.example.Auto_Grade.dto.req.UpdateAccountRequest;
import com.example.Auto_Grade.dto.req.UpdateAvatarRequest;
import com.example.Auto_Grade.dto.res.AccountResponse;
import com.example.Auto_Grade.dto.res.AvatarUrlResponse;
import com.example.Auto_Grade.dto.res.ProfilePersonalResponse;
import com.example.Auto_Grade.dto.res.UpdateAccountResponse;
import com.example.Auto_Grade.enums.Role;
import org.springframework.data.domain.Page;

public interface AccountService {
    void deleteAccountById(Long id);

    boolean updateLockStatus(Long id);

    void changePassword(ChangePasswordRequest changePasswordRequest);

    AvatarUrlResponse updateAvatarUrl(UpdateAvatarRequest request);

    UpdateAccountResponse updateAccount(UpdateAccountRequest updateAccountRequest);

    Page<AccountResponse> getAccountsByRole(Role role, int page, int size);

    ProfilePersonalResponse getProfilePersonal();
}
