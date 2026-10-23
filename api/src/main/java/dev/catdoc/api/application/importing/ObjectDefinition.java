package dev.catdoc.api.application.importing;

import java.util.Map;

public record ObjectDefinition(String id, String title, String domain, Map<String, Object> metadata) {

    public ObjectDefinition {
        metadata = Lists.orEmpty(metadata);
    }
}
