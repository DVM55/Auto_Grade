package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.ClassCodeRequest;
import com.example.Auto_Grade.dto.res.ClassMemberResponse;
import com.example.Auto_Grade.dto.res.MetaResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.Class;
import com.example.Auto_Grade.entity.ClassMember;
import com.example.Auto_Grade.enums.MemberStatus;
import com.example.Auto_Grade.integration.minio.MinioChannel;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.ClassMemberRepository;
import com.example.Auto_Grade.repository.ClassRepository;
import com.example.Auto_Grade.service.ClassMemberService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassMemberServiceImpl implements ClassMemberService {
    private final ClassMemberRepository classMemberRepository;
    private final AccountRepository accountRepository;
    private final ClassRepository classRepository;
    private final MinioChannel minioChannel;

    private Account getCurrentAccount() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + userId));
    }

    @Override
    public void joinClass(ClassCodeRequest request) {
        Account currentUser = getCurrentAccount();

        Class clazz = classRepository.findByClassCode(request.getClassCode())
                .orElseThrow(() ->
                        new EntityNotFoundException("Mã lớp không tồn tại"));

        if (clazz.getCreator().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn là người tạo lớp này");
        }

        if (classMemberRepository
                .existsByClassEntityIdAndAccountId(clazz.getId(), currentUser.getId())) {
            throw new IllegalArgumentException("Bạn đã gửi yêu cầu hoặc đã là thành viên");
        }

        // ✅ Tạo yêu cầu tham gia
        ClassMember classMember = ClassMember.builder()
                .classEntity(clazz)
                .account(currentUser)
                .status(MemberStatus.PENDING)
                .build();

        classMemberRepository.save(classMember);
    }

    @Override
    public void approveMember(Long id) {
        Account currentUser = getCurrentAccount();
        ClassMember classMember = classMemberRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy yêu cầu tham gia"));

        Class clazz = classMember.getClassEntity();

        // 🔒 Chỉ creator mới được duyệt
        if (!clazz.getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền phê duyệt thành viên");
        }

        // ✅ Cập nhật trạng thái
        classMember.setStatus(MemberStatus.APPROVED);

        classMemberRepository.save(classMember);
    }

    @Override
    public void removeMember(Long id) {

        Account currentUser = getCurrentAccount();

        ClassMember classMember = classMemberRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy thành viên"));

        Class clazz = classMember.getClassEntity();

        // 🔒 Chỉ creator mới được xóa
        if (!clazz.getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xóa thành viên");
        }

        classMemberRepository.delete(classMember);
    }

    @Override
    public PagingResponse<ClassMemberResponse> getPendingMembers(
            Long classId,
            String username,
            String email,
            int page,
            int size
    ) {

        // 1️⃣ Lấy class
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy lớp với id: " + classId)
                );

        // 2️⃣ Lấy user hiện tại
        Account currentUser = getCurrentAccount();

        // 3️⃣ Check quyền
        if (!classEntity.getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem danh sách này");
        }

        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ClassMember> pageResponse = classMemberRepository
                .findMembersByStatusAndFilters(
                        classId,
                        MemberStatus.PENDING,
                        username,
                        email,
                        pageable
                );

        MetaResponse meta = MetaResponse.builder()
                .totalItems(pageResponse.getTotalElements())
                .itemCount(pageResponse.getNumberOfElements())
                .itemsPerPage(pageResponse.getSize())
                .totalPages(pageResponse.getTotalPages())
                .currentPage(pageResponse.getNumber() + 1)
                .build();

        return PagingResponse.<ClassMemberResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy danh sách thành viên đợi phê duyệt thành công")
                .data(pageResponse.map(this::mapToResponse).getContent())
                .meta(meta)
                .build();
    }

    @Override
    public PagingResponse<ClassMemberResponse> getApprovedMembers(
            Long classId,
            String username,
            String email,
            int page,
            int size
    ) {

        // 1️⃣ Lấy class
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy lớp với id: " + classId)
                );

        // 2️⃣ Lấy user hiện tại
        Account currentUser = getCurrentAccount();

        // 3️⃣ Check quyền
        if (!classEntity.getCreator().getId().equals(currentUser.getId()) &&
                !classMemberRepository.existsByClassEntity_IdAndAccount_IdAndStatus(classId, currentUser.getId(), MemberStatus.APPROVED)) {
            throw new AccessDeniedException("Bạn không có quyền xem danh sách này");
        }

        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ClassMember> pageResponse = classMemberRepository
                .findMembersByStatusAndFilters(
                        classId,
                        MemberStatus.APPROVED,
                        username,
                        email,
                        pageable
                );

        MetaResponse meta = MetaResponse.builder()
                .totalItems(pageResponse.getTotalElements())
                .itemCount(pageResponse.getNumberOfElements())
                .itemsPerPage(pageResponse.getSize())
                .totalPages(pageResponse.getTotalPages())
                .currentPage(pageResponse.getNumber() + 1)
                .build();

        return PagingResponse.<ClassMemberResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy danh sách thành viên thành công")
                .data(pageResponse.map(this::mapToResponse).getContent())
                .meta(meta)
                .build();
    }

    private ClassMemberResponse mapToResponse(ClassMember member) {
        return ClassMemberResponse.builder()
                .id(member.getId())
                .accountId(member.getAccount().getId())
                .email(member.getAccount().getEmail())
                .username(member.getAccount().getUsername())
                .avatarUrl(minioChannel.getPresignedUrlSafe(member.getAccount().getObject_key(), 3600))
                .build();
    }


}
