package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.CategoryQuestionRequest;
import com.example.Auto_Grade.dto.res.CategoryQuestionResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.CategoryQuestion;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.CategoryQuestionRepository;
import com.example.Auto_Grade.service.CategoryQuestionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryQuestionServiceImpl implements CategoryQuestionService {

    private final CategoryQuestionRepository categoryQuestionRepository;
    private final AccountRepository accountRepository;

    // ─────────────────────── helpers ───────────────────────

    private Account getCurrentAccount() {
        Long id = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tài khoản không tồn tại"));
    }

    private void validateOwner(CategoryQuestion category, Account account) {
        if (!category.getCreator().getId().equals(account.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác danh mục này");
        }
    }

    private CategoryQuestionResponse mapToResponse(CategoryQuestion category) {
        return CategoryQuestionResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    // ─────────────────────── CRUD ───────────────────────────

    @Override
    @Transactional
    public void createCategoryQuestion(CategoryQuestionRequest request) {
        Account creator = getCurrentAccount();
        CategoryQuestion category = CategoryQuestion.builder()
                .name(request.getName())
                .creator(creator)
                .build();
        categoryQuestionRepository.save(category);
    }

    @Override
    @Transactional
    public void updateCategoryQuestion(Long id, CategoryQuestionRequest request) {
        CategoryQuestion category = categoryQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy danh mục với id: " + id));
        validateOwner(category, getCurrentAccount());
        category.setName(request.getName());
        categoryQuestionRepository.save(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CategoryQuestion category = categoryQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy danh mục với id: " + id));
        validateOwner(category, getCurrentAccount());
        categoryQuestionRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryQuestionResponse> getAllCategoryQuestionByCreatorId() {
        Account account = getCurrentAccount();

        List<CategoryQuestion> categories =
                categoryQuestionRepository.findAllByCreatorId(account.getId());

        return categories.stream()
                .map(this::mapToResponse)
                .toList();
    }

}

