package com.example.Auto_Grade.service.implementation;

import com.example.Auto_Grade.dto.req.ExamRequest;

import com.example.Auto_Grade.dto.res.ExamResponse;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.Exam;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.ExamRepository;
import com.example.Auto_Grade.service.ExamService;
import jakarta.persistence.EntityNotFoundException;
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
public class ExamServiceImpl implements ExamService {
    private final ExamRepository examRepository;
    private final AccountRepository accountRepository;

    @Override
    public void deleteExamById(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy đợt thi với id: " + id)
                );

        Account currentAccount = getCurrentAccount();

        // Kiểm tra quyền
        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xoá đợt thi này");
        }

        examRepository.delete(exam);
    }

    @Override
    public void createExam(ExamRequest examRequest) {
        if (examRepository.existsByName(examRequest.getName())) {
            throw new IllegalArgumentException("Tên đợt thi đã tồn tại");
        }

        Account currentAccount = getCurrentAccount();

        Exam exam = Exam.builder()
                .name(examRequest.getName())
                .creator(currentAccount)
                .build();

        examRepository.save(exam);
    }

    @Override
    public ExamResponse updateExam(Long id, ExamRequest examRequest) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy đợt thi với id: " + id)
                );

        Account currentAccount = getCurrentAccount();

        // Kiểm tra quyền
        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xoá đợt thi này");
        }

        if (examRepository.existsByName(examRequest.getName())) {
            throw new IllegalArgumentException("Tên đợt thi đã tồn tại");
        }

        exam.setName(examRequest.getName());
        examRepository.save(exam);

        return ExamResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }

    @Override
    public Page<ExamResponse> getExams(String name, int page, int size) {

        Account currentAccount = getCurrentAccount();

        // nếu name rỗng thì set null để query bỏ filter
        if (name != null && name.isBlank()) {
            name = null;
        }

        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Exam> examPage =
                examRepository.getExams(
                        currentAccount.getId(),
                        name,
                        pageable
                );

        return examPage.map(this::mapToResponse);
    }

    private ExamResponse mapToResponse(Exam exam) {

        return ExamResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }

    private Account getCurrentAccount() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + userId));
    }
}
