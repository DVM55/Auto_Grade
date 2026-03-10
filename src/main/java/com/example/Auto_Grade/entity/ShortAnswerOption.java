package com.example.Auto_Grade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "short_answer_options",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "answer_text"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortAnswerOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Một trong các đáp án đúng (không phân biệt hoa/thường khi so sánh).
     * VD: "teacher", "giáo viên", "thầy giáo" đều là đáp án hợp lệ.
     */
    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;
}

