package com.example.Auto_Grade.service.implementation;


import com.example.Auto_Grade.dto.req.ExamSessionRequest;
import com.example.Auto_Grade.dto.req.UpdateExamSessionRequest;
import com.example.Auto_Grade.dto.res.ExamSessionResponse;
import com.example.Auto_Grade.entity.Account;

import com.example.Auto_Grade.entity.Exam;
import com.example.Auto_Grade.entity.ExamSession;
import com.example.Auto_Grade.repository.AccountRepository;
import com.example.Auto_Grade.repository.ExamRepository;
import com.example.Auto_Grade.repository.ExamSessionRepository;
import com.example.Auto_Grade.service.ExamSessionService;
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
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionRepository examSessionRepository;
    private final AccountRepository accountRepository;
    private final ExamRepository examRepository;

    @Override
    public void createExamSession(Long examId, ExamSessionRequest request) {

        Account currentAccount = getCurrentAccount();

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy kỳ thi với id: " + examId));

        // Check quyền (chỉ creator được tạo session)
        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException("Bạn không có quyền tạo đợt thi cho kỳ thi này");
        }

        // Check trùng session name trong cùng exam
        if (examSessionRepository.existsByExamIdAndSessionName(
                examId, request.getSessionName())) {
            throw new IllegalArgumentException("Tên đợt thi đã tồn tại!");
        }

        ExamSession examSession = ExamSession.builder()
                .sessionName(request.getSessionName())
                .gradeType(request.getGradeType())
                .exam(exam)
                .build();

        examSessionRepository.save(examSession);
    }

    @Override
    public ExamSessionResponse updateExamSession(Long id, UpdateExamSessionRequest request) {

        Account currentAccount = getCurrentAccount();

        ExamSession examSession = examSessionRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy session với id: " + id));

        Exam exam = examSession.getExam();

        // Check quyền
        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa đọt thi này");
        }

        // Check trùng name (trừ chính nó)
        if (!examSession.getSessionName().equals(request.getSessionName())
                && examSessionRepository.existsByExamIdAndSessionName(
                exam.getId(), request.getSessionName())) {
            throw new IllegalArgumentException("Tên đợt thi đã tồn tại!");
        }

        examSession.setSessionName(request.getSessionName());

        examSessionRepository.save(examSession);

        return mapToResponse(examSession);
    }

    @Override
    public void deleteExamById(Long id) {

        Account currentAccount = getCurrentAccount();

        ExamSession examSession = examSessionRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy đọt thi với id: " + id));

        Exam exam = examSession.getExam();

        // Check quyền
        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xoá đợt thi này");
        }

        examSessionRepository.delete(examSession);
    }

    @Override
    public Page<ExamSessionResponse> getExamSessionByExamId(
            Long examId,
            String sessionName,
            int page,
            int size
    ) {

        Account currentAccount = getCurrentAccount();

        // 🔎 Kiểm tra exam tồn tại
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy kỳ thi với id: " + examId));

        // 🔒 Check quyền (creator mới được xem)
        if (!exam.getCreator().getId().equals(currentAccount.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem session của kỳ thi này");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<ExamSession> examSessionPage =
                examSessionRepository.getExamSessions(
                        examId,
                        sessionName,
                        pageable
                );

        return examSessionPage.map(this::mapToResponse);
    }

    private ExamSessionResponse mapToResponse(ExamSession examSession) {

        return ExamSessionResponse.builder()
                .id(examSession.getId())
                .sessionName(examSession.getSessionName())
                .gradeType(examSession.getGradeType().name())
                .createdAt(examSession.getCreatedAt())
                .updatedAt(examSession.getUpdatedAt())
                .build();
    }

    private Account getCurrentAccount() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return accountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản với id: " + userId));
    }


}
