package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.ClassRequest;
import com.example.Auto_Grade.dto.res.ClassDetailResponse;
import com.example.Auto_Grade.dto.res.ClassResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.Class;
import com.example.Auto_Grade.enums.JoinStatus;
import com.example.Auto_Grade.enums.MemberStatus;
import com.example.Auto_Grade.mapper.ClassMapper;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.ClassMemberRepository;
import com.example.Auto_Grade.repository.ClassRepository;
import com.example.Auto_Grade.service.ClassService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final AccountRepository accountRepository;
    private final ClassMapper classMapper;
    private final ClassMemberRepository classMemberRepository;

    @Override
    public ClassResponse createClass(ClassRequest request) {
        Account creator = getCurrentAccount();

        String code = generateUniqueClassCode();

        Class newClass = new Class();
        newClass.setTitle(request.getTitle());
        newClass.setDescription(request.getDescription());
        newClass.setClassCode(code);
        newClass.setCreator(creator);

        classRepository.save(newClass);

        return ClassResponse.builder()
                .id(newClass.getId())
                .classCode(code)
                .title(newClass.getTitle())
                .description(newClass.getDescription())
                .memberCount(0L)
                .build();
    }

    @Override
    public ClassResponse updateClass(ClassRequest request, Long classId) {
        Account currentUser = getCurrentAccount();

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp với id: " + classId));

        if (!clazz.getCreator().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền sửa đổi lớp này");
        }

        classMapper.updateClassFromDTO(request,clazz);

        classRepository.save(clazz);

        long approvedCount = classMemberRepository
                .countByClassEntityIdAndStatus(classId, MemberStatus.APPROVED);

        return ClassResponse.builder()
                .id(clazz.getId())
                .classCode(clazz.getClassCode())
                .title(clazz.getTitle())
                .description(clazz.getDescription())
                .memberCount(approvedCount)
                .build();
    }

    @Override
    public void deleteClass(Long classId) {
        Account currentUser = getCurrentAccount();

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp với id: " + classId));

        if (!clazz.getCreator().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền xoá lớp này");
        }

        classRepository.delete(clazz);
    }

    @Override
    public Page<ClassResponse> getClasses(
            String title,
            String classCode,
            int page,
            int size
    ) {

        Account currentUser = getCurrentAccount();

        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Class> classPage =
                classRepository.getClasses(
                        currentUser.getId(),
                        title,
                        classCode,
                        pageable
                );

        return classPage.map(this::mapToResponse);
    }

    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    public String generateUniqueClassCode() {
        String code;
        int maxRetry = 5;
        int attempt = 0;

        do {
            code = generateRandomCode();
            attempt++;
        } while (classRepository.existsByClassCode(code) && attempt < maxRetry);

        if (attempt == maxRetry) {
            throw new RuntimeException("Lỗi. Vui lòng thử lại.");
        }

        return code;
    }

    @Override
    public ClassDetailResponse getClassDetailByCode(String classCode) {

        Long userId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Class clazz = classRepository.findByClassCode(classCode)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy lớp với mã: " + classCode));

        Long memberCount = classMemberRepository
                .countByClassEntityIdAndStatus(clazz.getId(), MemberStatus.APPROVED);

        Optional<MemberStatus> statusOpt =
                classMemberRepository.findStatusByClassAndUser(clazz.getId(), userId);

        JoinStatus joinStatus = JoinStatus.NOT_JOINED;

        if (statusOpt.isPresent()) {
            if (statusOpt.get() == MemberStatus.APPROVED) {
                joinStatus = JoinStatus.JOINED;
            } else if (statusOpt.get() == MemberStatus.PENDING) {
                joinStatus = JoinStatus.PENDING_APPROVAL;
            }
        }

        return ClassDetailResponse.builder()
                .id(clazz.getId())
                .classCode(clazz.getClassCode())
                .title(clazz.getTitle())
                .description(clazz.getDescription())
                .memberCount(memberCount)
                .joinStatus(joinStatus)
                .build();
    }

    private ClassResponse mapToResponse(Class clazz) {

        long approvedCount = classMemberRepository
                .countByClassEntityIdAndStatus(clazz.getId(), MemberStatus.APPROVED);

        return ClassResponse.builder()
                .id(clazz.getId())
                .title(clazz.getTitle())
                .description(clazz.getDescription())
                .classCode(clazz.getClassCode())
                .memberCount(approvedCount)
                .build();
    }

    private Account getCurrentAccount() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + userId));
    }
}
