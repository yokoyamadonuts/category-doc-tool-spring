package dev.catdoc.api.application.importing;

import java.util.Map;

public record MorphismDefinition(String id, String name, String source, String target,
        Map<String, Object> metadata) {

    public MorphismDefinition {
        metadata = Lists.orEmpty(metadata);
    }
}
