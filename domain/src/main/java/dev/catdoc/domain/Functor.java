package dev.catdoc.domain;

import static dev.catdoc.domain.Validation.copyOrdered;
import static dev.catdoc.domain.Validation.requireNonBlank;

import java.util.Map;
import java.util.Optional;

/** カテゴリ間の対応。要件から仕様へ、のように文書と関係を移す。 */
public record Functor(String id, String name, String sourceCategory, String targetCategory,
        Map<String, String> objectMapping, Map<String, String> morphismMapping) {

    public Functor {
        requireNonBlank(id, "id");
        requireNonBlank(name, "name");
        requireNonBlank(sourceCategory, "sourceCategory");
        requireNonBlank(targetCategory, "targetCategory");
        objectMapping = copyOrdered(objectMapping);
        morphismMapping = copyOrdered(morphismMapping);
    }

    public Optional<String> mapObject(String objectId) {
        return Optional.ofNullable(objectMapping.get(objectId));
    }

    public Optional<String> mapMorphism(String morphismId) {
        return Optional.ofNullable(morphismMapping.get(morphismId));
    }
}
