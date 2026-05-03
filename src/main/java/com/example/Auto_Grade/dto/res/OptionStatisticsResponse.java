package com.example.Auto_Grade.dto.res;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionStatisticsResponse {

    private Long optionId;
    private String optionText;
    private boolean isCorrect;
    private long chosenCount;           // tổng số lần option này được chọn
    private double chosenPercent;       // % so với tổng số attempt
}