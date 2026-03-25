package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.CategoryQuestionRequest;
import com.example.Auto_Grade.dto.res.CategoryQuestionResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;



public interface CategoryQuestionService {
    void delete(Long categoryId);
    void createCategoryQuestion(CategoryQuestionRequest request);
    void updateCategoryQuestion(Long categoryId, CategoryQuestionRequest request);
    PagingResponse<CategoryQuestionResponse> getAllCategoryQuestionByCreatorId(
            int page,
            int size,
            String name
    );
}

