package com.example.Auto_Grade.dto.res;

import com.example.Auto_Grade.enums.MediaType;
import com.example.Auto_Grade.enums.QuestionType;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStatisticsResponse {

    private Long questionId;
    private String content;
    private MediaType mediaType;
    private String mediaUrl;
    private QuestionType questionType;

    private long correctCount;          // trả lời đúng
    private long wrongCount;            // trả lời sai
    private long skippedCount;          // bỏ trống

    private double correctPercent; // % trả lời đúng
    private double wrongPercent;
    private double skipPercent;

    // questionType == SINGLE_CHOICE
    private List<OptionStatisticsResponse> optionStats;
}