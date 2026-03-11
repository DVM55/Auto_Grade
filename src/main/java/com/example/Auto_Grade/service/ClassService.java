package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.ClassRequest;
import com.example.Auto_Grade.dto.res.ClassDetailResponse;
import com.example.Auto_Grade.dto.res.ClassResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;
import com.example.Auto_Grade.entity.Class;

public interface ClassService {
    void createClass(ClassRequest request);
    void updateClass(ClassRequest request, Long classId);
    void deleteClass(Long classId);

    PagingResponse<ClassResponse> getClassesByCreator(String title, String classCode, int page, int size);

    ClassDetailResponse getClassDetailByCode(String classCode);

    Class getClassById(Long id);

}
