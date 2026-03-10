package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ChangePasswordRequest;
import com.example.Auto_Grade.dto.req.UpdateAccountRequest;
import com.example.Auto_Grade.dto.req.UpdateAvatarRequest;
import com.example.Auto_Grade.dto.res.*;
import com.example.Auto_Grade.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {
    void deleteAccountById(Long id);

    boolean updateLockStatus(Long id);

    void changePassword(ChangePasswordRequest changePasswordRequest);

    AvatarUrlResponse updateAvatarUrl(UpdateAvatarRequest request);

    UpdateAccountResponse updateAccount(UpdateAccountRequest updateAccountRequest);

    PagingResponse<AccountResponse> getAccounts(Role role, int page, int size, String username, String email);

    ProfilePersonalResponse getProfilePersonal();
}
