package com.example.Auto_Grade.service;

import com.example.Auto_Grade.dto.req.GroupQuestionRequest;
import com.example.Auto_Grade.dto.res.GroupQuestionResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;



public interface GroupQuestionService {
    void delete(Long groupId);
    void createGroupQuestion(GroupQuestionRequest request);
    void updateGroupQuestion(Long groupId, GroupQuestionRequest request);

    PagingResponse<GroupQuestionResponse> getAllGroupQuestionByCreatorId(
            int page,
            int size,
            String name
    );
}

