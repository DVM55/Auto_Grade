package com.example.Auto_Grade.entity;

import com.example.Auto_Grade.enums.QuestionPartType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "answer_key_details",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_answer_detail",
                        columnNames = {
                                "answer_key_id",
                                "question_number",
                                "sub_question"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerKeyDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_type", nullable = false, length = 50)
    private QuestionPartType partType;

    @Column(name = "sub_question", length = 10)
    private String subQuestion;
    // null với PART1 & PART3
    // "a","b","c","d" với PART2

    @Column(name = "correct_value", nullable = false, length = 50)
    private String correctValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "answer_key_id",
            nullable = false
    )
    @JsonIgnore
    private AnswerKey answerKey;
}