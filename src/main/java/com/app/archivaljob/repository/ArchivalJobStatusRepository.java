package com.app.archivaljob.repository;

import com.app.archivaljob.domain.ArchivalJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchivalJobStatusRepository extends JpaRepository<ArchivalJobStatus, Integer> {
} 