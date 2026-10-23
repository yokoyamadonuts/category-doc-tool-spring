package dev.catdoc.domain;

import static dev.catdoc.domain.Validation.copyOrdered;
import static dev.catdoc.domain.Validation.requireNonBlank;

import java.util.Map;
import java.util.Optional;

/** 関手 F から G への変換。文書ごとに、F の対応先から G の対応先への射を成分として持つ。 */
public record NaturalTransformation(String id, String name, String sourceFunctor, String targetFunctor,
        Map<String, String> components) {

    public NaturalTransformation {
        requireNonBlank(id, "id");
        requireNonBlank(name, "name");
        requireNonBlank(sourceFunctor, "sourceFunctor");
        requireNonBlank(targetFunctor, "targetFunctor");
        components = copyOrdered(components);
    }

    public Optional<String> component(String objectId) {
        return Optional.ofNullable(components.get(objectId));
    }
}
