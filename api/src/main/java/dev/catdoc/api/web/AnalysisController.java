package dev.catdoc.api.web;

import dev.catdoc.api.application.AnalysisService;
import dev.catdoc.api.web.ProjectResponses.PathResult;
import dev.catdoc.api.web.ProjectResponses.TraceabilityTable;
import dev.catdoc.api.web.ProjectResponses.VerificationReport;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 検証、経路、追跡の表。どれも保存せず、呼ぶたびに計算する。 */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/verification")
    public VerificationReport verifyProject(@PathVariable UUID projectId) {
        return VerificationReport.from(analysisService.verify(projectId));
    }

    @GetMapping("/paths")
    public PathResult findPaths(@PathVariable UUID projectId, @RequestParam String from, @RequestParam String to) {
        return PathResult.from(analysisService.findPaths(projectId, from, to));
    }

    @GetMapping("/traceability")
    public TraceabilityTable traceProject(@PathVariable UUID projectId, @RequestParam List<String> functors) {
        return TraceabilityTable.from(analysisService.trace(projectId,
                functors.stream().map(String::strip).filter(s -> !s.isEmpty()).toList()));
    }
}
