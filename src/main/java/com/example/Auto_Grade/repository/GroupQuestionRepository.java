package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.GroupQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface GroupQuestionRepository extends JpaRepository<GroupQuestion, Long> {
    List<GroupQuestion> findAllByCreatorId(Long creatorId);
}

