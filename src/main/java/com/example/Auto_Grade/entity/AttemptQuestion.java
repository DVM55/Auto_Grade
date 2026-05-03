package com.example.Auto_Grade.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "attempt_questions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"attempt_id", "question_id"}
        )
)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptQuestion extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
