package dev.catdoc.api.web;

import dev.catdoc.api.application.ProjectSummary;
import java.time.Instant;
import java.util.UUID;

/** openapi.yaml の ProjectSummary に対応する。 */
public record ProjectSummaryResponse(UUID id, String name, Instant importedAt) {

    static ProjectSummaryResponse from(ProjectSummary summary) {
        return new ProjectSummaryResponse(summary.id(), summary.name(), summary.importedAt());
    }
}
