package com.service.domain.analytics.repository;

import com.service.domain.analytics.entity.AnalysisAssetSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisAssetSnapshotRepository
    extends JpaRepository<AnalysisAssetSnapshot, Long> {}
