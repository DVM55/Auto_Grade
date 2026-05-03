package com.example.Auto_Grade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_answers",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"attempt_id", "question_id"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnore
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    // SHORT_ANSWER → lưu text vào đây
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    // SINGLE_CHOICE / MULTIPLE_CHOICE → lưu option được chọn
    @ManyToMany
    @JoinTable(
            name = "quiz_answer_options",
            joinColumns = @JoinColumn(name = "answer_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id")
    )
    @Builder.Default
    private List<QuestionOption> selectedOptions = new ArrayList<>();

    @Column(name = "is_correct")
    private Boolean isCorrect;
}