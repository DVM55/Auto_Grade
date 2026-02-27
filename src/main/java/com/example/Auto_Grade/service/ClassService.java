package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ClassRequest;
import com.example.Auto_Grade.dto.res.ClassDetailResponse;
import com.example.Auto_Grade.dto.res.ClassResponse;
import com.example.Auto_Grade.entity.Class;
import org.springframework.data.domain.Page;

public interface ClassService {
    ClassResponse createClass(ClassRequest request);
    ClassResponse updateClass(ClassRequest request, Long classId);
    void deleteClass(Long classId);
    Page<ClassResponse> getClasses(
            String title,
            String classCode,
            int page,
            int size
    );

    ClassDetailResponse getClassDetailByCode(String classCode);

    Class getClassById(Long id);

}
