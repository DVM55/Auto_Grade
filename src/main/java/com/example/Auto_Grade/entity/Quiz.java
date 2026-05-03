package com.example.Auto_Grade.entity;

import com.example.Auto_Grade.enums.QuizAccessType;
import com.example.Auto_Grade.enums.QuizStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_code", nullable = false, unique = true, length = 20)
    private String quizCode;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "allow_review", nullable = false)
    @Builder.Default
    private Boolean allowReview = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnore
    private Account creator;

    @ManyToMany
    @JoinTable(
            name = "quiz_classes",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    @Builder.Default
    @JsonIgnore
    private List<Class> classes = new ArrayList<>();

    @Column(name = "auto_score", nullable = false)
    @Builder.Default
    private Boolean autoScore = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private QuizStatus status = QuizStatus.DRAFT;

    @Column(name = "is_random", nullable = false)
    private Boolean isRandom = false;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Question> questions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizAccessType accessType;
}

