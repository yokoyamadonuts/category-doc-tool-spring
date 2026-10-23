package dev.catdoc.api.application.importing;

import java.util.List;

public record CategoryDefinition(String id, String name, List<ObjectDefinition> objects,
        List<MorphismDefinition> morphisms) {

    public CategoryDefinition {
        objects = Lists.orEmpty(objects);
        morphisms = Lists.orEmpty(morphisms);
    }
}
