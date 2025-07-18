package com.app.archivaljob.batch.reader;

import com.app.archivaljob.domain.ArchivalQueryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DynamicQueryItemReader implements ItemReader<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(DynamicQueryItemReader.class);
    private final JdbcTemplate jdbcTemplate;
    private final StepExecution stepExecution;
    private Iterator<Map<String, Object>> dataIterator;

    public DynamicQueryItemReader(JdbcTemplate jdbcTemplate, StepExecution stepExecution) {
        this.jdbcTemplate = jdbcTemplate;
        this.stepExecution = stepExecution;
    }

    @Override
    public Map<String, Object> read() throws Exception {
        if (dataIterator == null) {
            log.info("[Batch] Initializing DynamicQueryItemReader for step: {}", stepExecution.getStepName());
            ExecutionContext ctx = stepExecution.getJobExecution().getExecutionContext();
            Object configObj = ctx.get("archivalQueryConfig");
            if (!(configObj instanceof ArchivalQueryConfig)) {
                log.error("[Batch] archivalQueryConfig not found or invalid in execution context");
                throw new IllegalStateException("archivalQueryConfig not found or invalid in execution context");
            }
            ArchivalQueryConfig config = (ArchivalQueryConfig) configObj;
            String query = config.getArchivalQuery();
            log.info("[Batch] Executing archival query: {}", query);
            List<Map<String, Object>> data = jdbcTemplate.queryForList(query);
            log.info("[Batch] Query returned {} records", data.size());
            dataIterator = data.iterator();
        }
        if (dataIterator.hasNext()) {
            return dataIterator.next();
        } else {
            log.info("[Batch] No more records to read.");
            return null;
        }
    }
} 