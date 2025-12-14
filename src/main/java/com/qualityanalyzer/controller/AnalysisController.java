package com.qualityanalyzer.controller;

import com.qualityanalyzer.dto.AnalyzeRequest;
import com.qualityanalyzer.dto.QualityReportDto;
import com.qualityanalyzer.model.AnalysisReport;
import com.qualityanalyzer.service.AnalysisOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisOrchestratorService orchestratorService;

    public AnalysisController(AnalysisOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@Valid @RequestBody AnalyzeRequest request) {
        try {
            QualityReportDto report = orchestratorService.analyze(request);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }

    @GetMapping("/reports")
    public List<Map<String, Object>> getRecentReports() {
        return orchestratorService.getRecentReports().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("rootUrl", r.getRootUrl());
            m.put("status", r.getStatus());
            m.put("overallScore", r.getOverallScore());
            m.put("pagesCrawled", r.getPagesCrawled());
            m.put("createdAt", r.getCreatedAt().toString());
            return m;
        }).toList();
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<?> getReport(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orchestratorService.getReportById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "Website Quality Analyzer");
    }
}
