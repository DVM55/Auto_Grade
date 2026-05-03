package com.example.Auto_Grade.dto.req;

import com.example.Auto_Grade.enums.QuizAccessType;
import com.example.Auto_Grade.enums.QuizStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuizRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Tổng điểm phải lớn hơn 0")
    @NotNull(message = "Tổng điểm không được để trống")
    private Double totalScore;

    @Min(value = 1, message = "Thời gian làm bài phải lớn hơn 0 phút")
    private Integer durationMinutes;

    @Min(value = 1, message = "Số lần làm bài tối thiểu là 1")
    @NotNull(message = "Số lần làm bài không được để trống")
    private Integer maxAttempts;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Boolean allowReview = false;

    private Boolean isRandom = false;

    private QuizStatus quizStatus = QuizStatus.DRAFT;

    private QuizAccessType quizAccessType = QuizAccessType.PUBLIC;

    private Boolean autoScore = true;

    // classId tuỳ chọn – nếu null thì quiz không gắn lớp
    private List<Long> classId;

    // Thủ công
    @Valid
    private List<QuizQuestionRequest> questions;

    // Random từ ngân hàng
    @Valid
    private List<QuizQuestionConfigRequest> randomConfigs;
}

