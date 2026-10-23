package dev.catdoc.api.application.importing;

import java.util.Map;

public record NaturalTransformationDefinition(String id, String name, String sourceFunctor, String targetFunctor,
        Map<String, String> components) {

    public NaturalTransformationDefinition {
        components = Lists.orEmpty(components);
    }
}
