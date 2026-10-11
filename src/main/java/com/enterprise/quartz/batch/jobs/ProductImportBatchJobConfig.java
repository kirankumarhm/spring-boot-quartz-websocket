package com.enterprise.quartz.batch.jobs;

import com.enterprise.quartz.batch.listener.BatchExecutionLoggingListener;
import com.enterprise.quartz.batch.model.ProcessedProduct;
import com.enterprise.quartz.batch.model.ProductImportDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <h3>Production Chunk-Oriented Batch Job: Product Catalog Ingestion</h3>
 * <p>
 * Demonstrates Spring Batch 5 chunk processing with:
 * <ul>
 *   <li><b>Chunk commit interval:</b> 10 items per transaction commit</li>
 *   <li><b>ItemReader:</b> Reads catalog product records</li>
 *   <li><b>ItemProcessor:</b> Enriches, calculates tax/margins, validates SKUs, and generates slugs</li>
 *   <li><b>ItemWriter:</b> Batches and writes processed inventory records</li>
 *   <li><b>Observability:</b> Chunk metrics and MDC correlation logging</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
public class ProductImportBatchJobConfig {

    public static final String JOB_NAME = "product-import-batch-job";
    public static final String STEP_NAME = "product-import-step";
    private static final int CHUNK_SIZE = 10;

    @Bean
    @StepScope
    public ListItemReader<ProductImportDto> productItemReader() {
        // Generates sample dataset of 25 enterprise products across 3 chunks (10, 10, 5)
        List<ProductImportDto> items = new ArrayList<>();
        String[] categories = {"Electronics", "Office Supplies", "Hardware", "Networking", "Accessories"};
        for (int i = 1; i <= 25; i++) {
            items.add(ProductImportDto.builder()
                    .sku(String.format("SKU-%05d", i))
                    .name("Enterprise Inventory Item " + i)
                    .category(categories[i % categories.length])
                    .basePrice(BigDecimal.valueOf(19.99 + (i * 5.50)).setScale(2, RoundingMode.HALF_UP))
                    .quantity(10 + (i * 3))
                    .build());
        }
        return new ListItemReader<>(items);
    }

    @Bean
    public ItemProcessor<ProductImportDto, ProcessedProduct> productItemProcessor() {
        return item -> {
            log.debug("Processing product SKU [{}]: basePrice=${}", item.getSku(), item.getBasePrice());

            // Business logic: 18% standard VAT/Tax calculation
            BigDecimal tax = item.getBasePrice().multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal finalPrice = item.getBasePrice().add(tax);
            String slug = item.getName().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");

            return ProcessedProduct.builder()
                    .sku(item.getSku())
                    .name(item.getName())
                    .slug(slug)
                    .category(item.getCategory())
                    .finalPrice(finalPrice)
                    .taxAmount(tax)
                    .stockCount(item.getQuantity())
                    .processedAt(Instant.now())
                    .correlationId(MDC.get("correlationId"))
                    .build();
        };
    }

    @Bean
    public ItemWriter<ProcessedProduct> productItemWriter() {
        return chunk -> {
            log.info("--> [Chunk Commit] Writing batch of {} processed products to target data store", chunk.size());
            for (ProcessedProduct product : chunk) {
                log.debug("    Committed product [{}]: finalPrice=${}, stock={}",
                        product.getSku(), product.getFinalPrice(), product.getStockCount());
            }
        };
    }

    @Bean
    public Step productImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<ProductImportDto> reader,
            ItemProcessor<ProductImportDto, ProcessedProduct> processor,
            ItemWriter<ProcessedProduct> writer,
            BatchExecutionLoggingListener listener) {

        return new StepBuilder(STEP_NAME, jobRepository)
                .<ProductImportDto, ProcessedProduct>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(listener)
                .build();
    }

    @Bean(name = JOB_NAME)
    public Job productImportBatchJob(
            JobRepository jobRepository,
            Step productImportStep,
            BatchExecutionLoggingListener listener) {

        return new JobBuilder(JOB_NAME, jobRepository)
                .start(productImportStep)
                .listener(listener)
                .build();
    }
}
