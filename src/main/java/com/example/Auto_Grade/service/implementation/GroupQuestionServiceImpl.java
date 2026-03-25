package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.GroupQuestionRequest;
import com.example.Auto_Grade.dto.res.GroupQuestionResponse;
import com.example.Auto_Grade.dto.res.MetaResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.GroupQuestion;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.GroupQuestionRepository;
import com.example.Auto_Grade.service.GroupQuestionService;
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
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class GroupQuestionServiceImpl implements GroupQuestionService {

    private final GroupQuestionRepository groupRepository;
    private final AccountRepository accountRepository;

    // ─────────────────────── helpers ───────────────────────

    private Account getCurrentAccount() {
        Long id = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tài khoản không tồn tại"));
    }

    private void validateOwner(GroupQuestion group, Account account) {
        if (!group.getCreator().getId().equals(account.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác nhóm câu hỏi này");
        }
    }

    private GroupQuestionResponse mapToResponse(GroupQuestion group) {
        return GroupQuestionResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    // ─────────────────────── CRUD ───────────────────────────

    @Override
    @Transactional
    public void createGroupQuestion(GroupQuestionRequest request) {
        Account creator = getCurrentAccount();

        GroupQuestion group = GroupQuestion.builder()
                .name(request.getName())
                .creator(creator)
                .build();

        groupRepository.save(group);
    }

    @Override
    @Transactional
    public void updateGroupQuestion(Long groupId, GroupQuestionRequest request) {
        GroupQuestion group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy nhóm câu hỏi với id: " + groupId));

        validateOwner(group, getCurrentAccount());

        group.setName(request.getName());

        groupRepository.save(group);
    }

    @Override
    @Transactional
    public void delete(Long groupId) {
        GroupQuestion group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy nhóm câu hỏi với id: " + groupId));

        validateOwner(group, getCurrentAccount());

        groupRepository.delete(group);
    }

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<GroupQuestionResponse> getAllGroupQuestionByCreatorId(
            int page,
            int size,
            String name
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

        Account account = getCurrentAccount();

        String keyword = normalizeKeyword(name);

        Page<GroupQuestion> groupPage;

       if (hasAccent(keyword)) {
            // có dấu
            groupPage = groupRepository.searchWithAccent(account.getId(), keyword, pageable);
        } else {
            // không dấu
            groupPage = groupRepository.findAllByCreatorId(account.getId(), keyword, pageable);
        }

        MetaResponse meta = MetaResponse.builder()
                .totalItems(groupPage.getTotalElements())
                .itemCount(groupPage.getNumberOfElements())
                .itemsPerPage(groupPage.getSize())
                .totalPages(groupPage.getTotalPages())
                .currentPage(groupPage.getNumber() + 1)
                .build();

        return PagingResponse.<GroupQuestionResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy danh sách group thành công")
                .data(groupPage.map(this::mapToResponse).getContent())
                .meta(meta)
                .build();
    }

    public String normalizeKeyword(String str) {
        if (str == null) return null;

        return str
                .replaceAll("\\s+", " ") // nhiều space → 1 space
                .trim();                // bỏ space đầu cuối
    }

    public boolean hasAccent(String str) {
        if (str == null) return false;

        // normalize về dạng decomposed
        String normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);

        // nếu có ký tự dấu (diacritics) thì return true
        return normalized.matches(".*\\p{InCombiningDiacriticalMarks}+.*");
    }
}