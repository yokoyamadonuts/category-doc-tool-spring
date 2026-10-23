package dev.catdoc.api.application.importing;

import java.util.Map;

public record FunctorDefinition(String id, String name, String sourceCategory, String targetCategory,
        Map<String, String> objectMapping, Map<String, String> morphismMapping) {

    public FunctorDefinition {
        objectMapping = Lists.orEmpty(objectMapping);
        morphismMapping = Lists.orEmpty(morphismMapping);
    }
}
