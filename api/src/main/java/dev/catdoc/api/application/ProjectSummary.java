package dev.catdoc.api.application;

import java.time.Instant;
import java.util.UUID;

/** プロジェクトの一覧に並べる1行。 */
public record ProjectSummary(UUID id, String name, Instant importedAt) {}
