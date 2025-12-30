package com.ademarli.municipality_service.repository;

import com.ademarli.municipality_service.model.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
}

