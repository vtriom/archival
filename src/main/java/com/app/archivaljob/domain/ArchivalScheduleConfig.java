package com.app.archivaljob.domain;

import java.time.LocalDateTime;

public class ArchivalScheduleConfig {
    private Integer id;

    private ArchivalQueryConfig queryConfig;

    private String cronExpression;

    private String status;

    private LocalDateTime lastRunAt;

    private LocalDateTime createdAt;

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ArchivalQueryConfig getQueryConfig() { return queryConfig; }
    public void setQueryConfig(ArchivalQueryConfig queryConfig) { this.queryConfig = queryConfig; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 