package com.qualityanalyzer.repository;

import com.qualityanalyzer.model.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    List<AnalysisReport> findTop10ByOrderByCreatedAtDesc();
}
