package com.linksphere.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickTimeSeriesPoint {
    private String date; // YYYY-MM-DD
    private long clicks;
}
