package dev.catdoc.api.application;

import java.time.Instant;
import java.util.UUID;

/** 1回の取り込み。 */
public record Project(UUID id, String name, Instant importedAt, ProjectGraph graph) {

    public ProjectSummary summary() {
        return new ProjectSummary(id, name, importedAt);
    }
}
