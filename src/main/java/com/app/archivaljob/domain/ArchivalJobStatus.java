package com.app.archivaljob.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "archival_job_status")
public class ArchivalJobStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_config_id", nullable = false)
    private ArchivalScheduleConfig scheduleConfig;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "status")
    private String status;

    @Column(name = "parquet_file_path")
    private String parquetFilePath;

    @Column(name = "created_at")
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