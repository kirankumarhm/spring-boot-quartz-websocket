package com.enterprise.quartz.batch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transformed and validated product model ready for database persistence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedProduct {
    private String sku;
    private String name;
    private String slug;
    private String category;
    private BigDecimal finalPrice;
    private BigDecimal taxAmount;
    private int stockCount;
    private Instant processedAt;
    private String correlationId;
}
