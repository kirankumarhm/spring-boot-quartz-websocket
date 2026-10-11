package com.enterprise.quartz.batch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Raw input record simulating an external catalog product payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportDto {
    private String sku;
    private String name;
    private String category;
    private BigDecimal basePrice;
    private int quantity;
}
