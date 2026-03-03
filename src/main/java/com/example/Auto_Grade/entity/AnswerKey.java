package com.example.Auto_Grade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


import java.util.List;

@Entity
@Table(
        name = "answer_keys",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_answer_keys_exam_paper",
                        columnNames = {"exam_session_id", "paper_code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerKey extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paper_code", nullable = false, length = 50)
    private String paperCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    @JsonIgnore
    private ExamSession examSession;

    @OneToMany(
            mappedBy = "answerKey",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AnswerKeyDetail> details;
}