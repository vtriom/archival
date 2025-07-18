package com.app.archivaljob.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "archival_schedule_config")
public class ArchivalScheduleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_config_id", nullable = false)
    private ArchivalQueryConfig queryConfig;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(name = "status")
    private String status;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "created_at")
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