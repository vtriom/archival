package com.app.archivaljob.batch;

import com.app.archivaljob.batch.reader.DynamicQueryItemReader;
import com.app.archivaljob.batch.writer.ParquetS3ItemWriter;
import com.app.archivaljob.domain.ArchivalQueryConfig;
import com.app.archivaljob.repository.ArchivalQueryConfigRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.core.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Configuration
public class ArchivalJobConfig {
    private static final Logger log = LoggerFactory.getLogger(ArchivalJobConfig.class);

    @Bean
    public Job archivalJob(JobRepository jobRepository, Step fetchQueryStep, Step processAndExportStep) {
        return new org.springframework.batch.core.job.builder.JobBuilder("archivalJob", jobRepository)
                .start(fetchQueryStep)
                .next(processAndExportStep)
                .build();
    }

    @Bean
    public Step fetchQueryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet fetchQueryTasklet) {
        return new org.springframework.batch.core.step.builder.StepBuilder("fetchQueryStep", jobRepository)
                .tasklet(fetchQueryTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step processAndExportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                     ItemReader<Map<String, Object>> dynamicQueryItemReader,
                                     ItemWriter<Map<String, Object>> parquetS3ItemWriter) {
        return new org.springframework.batch.core.step.builder.StepBuilder("processAndExportStep", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(1000, transactionManager)
                .reader(dynamicQueryItemReader)
                .writer(parquetS3ItemWriter)
//                .faultTolerant()
//                .retryLimit(3)
//                .retry(Exception.class)
//                .skipLimit(10)
//                .skip(Exception.class)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet fetchQueryTasklet(@Autowired ArchivalQueryConfigRepository queryConfigRepository) {
        return (StepContribution contribution, ChunkContext chunkContext) -> {
            JobParameters jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
            Object queryIdObj = jobParameters.getParameters().get("queryId").getValue();
            log.info("[Batch] fetchQueryTasklet started for queryId={}", queryIdObj);
            if (queryIdObj == null) {
                throw new IllegalArgumentException("queryId parameter is required");
            }
            Integer queryId = Integer.parseInt(queryIdObj.toString());
            ArchivalQueryConfig config = queryConfigRepository.findById(queryId)
                    .orElseThrow(() -> new IllegalArgumentException("No query config found for id: " + queryId));
            // Store the entire config object in the job execution context
            chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                    .put("archivalQueryConfig", config);
            return org.springframework.batch.repeat.RepeatStatus.FINISHED;
        };
    }

    @Bean
    @StepScope
    public ItemReader<Map<String, Object>> dynamicQueryItemReader(JdbcTemplate jdbcTemplate, @Value("#{stepExecution}") org.springframework.batch.core.StepExecution stepExecution) {
        return new DynamicQueryItemReader(jdbcTemplate, stepExecution);
    }

    @Bean
    public ItemWriter<Map<String, Object>> parquetS3ItemWriter() {
        return new ParquetS3ItemWriter();
    }
} 