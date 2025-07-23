package com.app.archivaljob.domain;

import java.time.LocalDateTime;
import java.io.Serializable;

public class ArchivalQueryConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;

    private String queryName;

    private String archivalQuery;

    private String description;

    private LocalDateTime createdAt;

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getQueryName() { return queryName; }
    public void setQueryName(String queryName) { this.queryName = queryName; }
    public String getArchivalQuery() { return archivalQuery; }
    public void setArchivalQuery(String archivalQuery) { this.archivalQuery = archivalQuery; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 