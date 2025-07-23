package com.app.archivaljob.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ArchivalJobStatus {
    private Integer id;

    private ArchivalScheduleConfig scheduleConfig;

    private LocalDate runDate;

    private String status;

    private String parquetFilePath;

    private LocalDateTime createdAt;

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ArchivalScheduleConfig getScheduleConfig() { return scheduleConfig; }
    public void setScheduleConfig(ArchivalScheduleConfig scheduleConfig) { this.scheduleConfig = scheduleConfig; }
    public LocalDate getRunDate() { return runDate; }
    public void setRunDate(LocalDate runDate) { this.runDate = runDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getParquetFilePath() { return parquetFilePath; }
    public void setParquetFilePath(String parquetFilePath) { this.parquetFilePath = parquetFilePath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 