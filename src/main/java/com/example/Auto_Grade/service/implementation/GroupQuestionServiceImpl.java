package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.GroupQuestionRequest;
import com.example.Auto_Grade.dto.res.GroupQuestionResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.GroupQuestion;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.GroupQuestionRepository;
import com.example.Auto_Grade.service.GroupQuestionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public List<GroupQuestionResponse> getAllGroupQuestionByCreatorId() {

        Account account = getCurrentAccount();

        List<GroupQuestion> groups =
                groupRepository.findAllByCreatorId(account.getId());

        return groups.stream()
                .map(this::mapToResponse)
                .toList();
    }
}