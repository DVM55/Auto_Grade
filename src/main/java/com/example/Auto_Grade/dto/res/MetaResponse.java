package com.example.Auto_Grade.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaResponse {
    private long totalItems;
    private int itemCount;
    private int itemsPerPage;
    private int totalPages;
    private int currentPage;
}
