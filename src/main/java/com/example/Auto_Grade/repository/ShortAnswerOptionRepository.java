package com.example.Auto_Grade.repository;

import com.example.Auto_Grade.entity.ShortAnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShortAnswerOptionRepository extends JpaRepository<ShortAnswerOption, Long> {
}

