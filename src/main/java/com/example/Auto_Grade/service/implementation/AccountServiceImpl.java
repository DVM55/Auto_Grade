package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.ChangePasswordRequest;
import com.example.Auto_Grade.dto.req.UpdateAccountRequest;
import com.example.Auto_Grade.dto.req.UpdateAvatarRequest;
import com.example.Auto_Grade.dto.res.AccountResponse;
import com.example.Auto_Grade.dto.res.AvatarUrlResponse;
import com.example.Auto_Grade.dto.res.ProfilePersonalResponse;
import com.example.Auto_Grade.dto.res.UpdateAccountResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.UserDetail;
import com.example.Auto_Grade.enums.Role;
import com.example.Auto_Grade.integration.minio.MinioChannel;
import com.example.Auto_Grade.mapper.AccountMapper;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.UserDetailRepository;
import com.example.Auto_Grade.service.AccountService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioChannel minioChannel;
    private final UserDetailRepository userDetailRepository;
    private final AccountMapper accountMapper;

    @Override
    public void deleteAccountById(Long id) {
        accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + id));
        accountRepository.deleteById(id);
    }

    @Override
    public boolean updateLockStatus(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + id));
        boolean newLockedState = !account.isLocked(); // đảo trạng thái

        account.setLocked(newLockedState);
        accountRepository.save(account);

        return newLockedState;
    }

    @Override
    public void changePassword(ChangePasswordRequest req) {
        Account currentAccount = getCurrentAccount();
        if (!passwordEncoder.matches(req.getOldPassword(), currentAccount.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }

        currentAccount.setPassword(passwordEncoder.encode(req.getNewPassword()));
        accountRepository.save(currentAccount);
    }

    @Override
    public AvatarUrlResponse updateAvatarUrl(UpdateAvatarRequest request) {
        Account currentAccount = getCurrentAccount();

        MultipartFile file = request.getFile();

        validateImage(file);

        try (InputStream is = file.getInputStream()) {

            // Upload file lên MinIO
            Map<String, String> result = minioChannel.uploadFile(
                    file.getOriginalFilename(),
                    is,
                    file.getSize(),
                    file.getContentType(),
                    3600
            );
            String objectKey = result.get("objectKey");

            // Cập nhật DB
            currentAccount.setObject_key(objectKey);
            accountRepository.save(currentAccount);

            return AvatarUrlResponse.builder()
                    .avatarUrl(result.get("url"))
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc dữ liệu từ file upload", e);

        } catch (Exception e) {
            throw new RuntimeException("Upload avatar thất bại", e);
        }

    }

    @Override
    public UpdateAccountResponse updateAccount(UpdateAccountRequest req) {
        Account currentAccount = getCurrentAccount();

        if (!currentAccount.getUsername().equals(req.getUsername())
                && accountRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Tên người dùng đã tồn tại");
        }

        if (!currentAccount.getEmail().equals(req.getEmail())
                && accountRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        // cập nhật dữ liệu account
        accountMapper.updateAccountFromDTO(req, currentAccount);

        // lấy hoặc tạo mới UserDetail
        UserDetail userDetail = userDetailRepository
                .findByAccountId(currentAccount.getId())
                .orElseGet(() -> {
                    UserDetail ud = new UserDetail();
                    ud.setAccount(currentAccount);
                    return ud;
                });

        // cập nhật dữ liệu userDetail
        accountMapper.updateUserDetailFromDTO(req, userDetail);

        // save
        userDetailRepository.save(userDetail);
        accountRepository.save(currentAccount);

        return UpdateAccountResponse.builder()
                .id(currentAccount.getId())
                .username(currentAccount.getUsername())
                .email(currentAccount.getEmail())
                .phone(userDetail.getPhone())
                .address(userDetail.getAddress())
                .gender(userDetail.getGender().name())
                .date_of_birth(userDetail.getDate_of_birth())
                .build();
    }

    @Override
    public Page<AccountResponse> getAccountsByRole(Role role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Account> accountPage = accountRepository.findByRole(role, pageable);

        return accountPage.map(account -> AccountResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .username(account.getUsername())
                .avatarUrl(minioChannel.getPresignedUrlSafe(account.getObject_key(), 3600))
                .role(account.getRole())
                .locked(account.isLocked())
                .build());
    }

    @Override
    public ProfilePersonalResponse getProfilePersonal(){
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String,Object> map = accountRepository.findAccountDetail(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + currentUserId));
        System.out.println(map.get("object_key"));
        return ProfilePersonalResponse.builder()
                .id((Long) map.get("id"))
                .username((String) map.get("username"))
                .email((String) map.get("email"))
                .avatarUrl(minioChannel.getPresignedUrlSafe((String) map.get("object_key"), 86400))
                .phone((String) map.get("phone"))
                .date_of_birth((LocalDate) map.get("date_of_birth"))
                .address((String) map.get("address"))
                .gender((String) map.get("gender"))
                .build();
    }

    public void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !(contentType.equals("image/jpeg")
                        || contentType.equals("image/png")
                        || contentType.equals("image/jpg")
                        || contentType.equals("image/webp"))) {

            throw new IllegalArgumentException("Chỉ cho phép upload file ảnh (jpg, png, webp)");
        }
    }

    private Account getCurrentAccount() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + userId));
    }
}
