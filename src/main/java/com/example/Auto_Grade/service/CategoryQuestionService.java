package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.CategoryQuestionRequest;
import com.example.Auto_Grade.dto.res.CategoryQuestionResponse;

import java.util.List;

public interface CategoryQuestionService {
    void delete(Long categoryId);
    void createCategoryQuestion(CategoryQuestionRequest request);
    void updateCategoryQuestion(Long categoryId, CategoryQuestionRequest request);
    List<CategoryQuestionResponse> getAllCategoryQuestionByCreatorId();
}

