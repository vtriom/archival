package com.app.archivaljob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableBatchProcessing
public class ArchivalJobApplication {
    private static final Logger log = LoggerFactory.getLogger(ArchivalJobApplication.class);
    public static void main(String[] args) {
        log.info("[Batch] ArchivalJobApplication main started");
        SpringApplication.run(ArchivalJobApplication.class, args);
    }

    @Bean
    public CommandLineRunner runJob(JobLauncher jobLauncher, Job archivalJob, @Value("${queryId:1}") String queryId) {
        return args -> {
            try {
                log.info("[Batch] Launching archivalJob with queryId={}", queryId);
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("queryId", queryId)
                        .toJobParameters();
                JobExecution execution = jobLauncher.run(archivalJob, jobParameters);
                log.info("[Batch] Job finished with status: {}", execution.getStatus());
            } catch (Exception e) {
                log.error("[Batch] Job failed to execute", e);
            }
        };
    }
} 