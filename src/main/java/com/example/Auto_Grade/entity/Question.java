package com.example.Auto_Grade.entity;

import com.example.Auto_Grade.enums.MediaType;
import com.example.Auto_Grade.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(name = "score")
    private Double score;

    @Column(name = "media_object_key", length = 500)
    private String mediaObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 20)
    private MediaType mediaType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    @JsonIgnore
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @JsonIgnore
    private Account creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_question_id")
    @JsonIgnore
    private GroupQuestion groupQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_question_id")
    @JsonIgnore
    private CategoryQuestion categoryQuestion;

    // Dùng cho SINGLE_CHOICE và MULTIPLE_CHOICE
    @Builder.Default
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<QuestionOption> options = new ArrayList<>();

    // Dùng cho SHORT_ANSWER – nhiều đáp án đúng
    @Builder.Default
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ShortAnswerOption> shortAnswerOptions = new ArrayList<>();
}

