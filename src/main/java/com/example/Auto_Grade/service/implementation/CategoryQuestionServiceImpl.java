package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.CategoryQuestionRequest;
import com.example.Auto_Grade.dto.res.CategoryQuestionResponse;
import com.example.Auto_Grade.dto.res.MetaResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.CategoryQuestion;

import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.CategoryQuestionRepository;
import com.example.Auto_Grade.service.CategoryQuestionService;
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
    public PagingResponse<CategoryQuestionResponse> getAllCategoryQuestionByCreatorId(
            int page,
            int size,
            String name
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

        Account account = getCurrentAccount();

        String keyword = normalizeKeyword(name);

        Page<CategoryQuestion> categoryPage;

        if (!hasAccent(keyword)) {
            // không dấu
            categoryPage = categoryQuestionRepository.findAllByCreatorId(account.getId(), keyword, pageable);
        } else {
            categoryPage = categoryQuestionRepository.searchWithAccent(account.getId(), keyword, pageable);
        }

        MetaResponse meta = MetaResponse.builder()
                .totalItems(categoryPage.getTotalElements())
                .itemCount(categoryPage.getNumberOfElements())
                .itemsPerPage(categoryPage.getSize())
                .totalPages(categoryPage.getTotalPages())
                .currentPage(categoryPage.getNumber() + 1)
                .build();

        return PagingResponse.<CategoryQuestionResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Lấy danh sách category thành công")
                .data(categoryPage.map(this::mapToResponse).getContent())
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

