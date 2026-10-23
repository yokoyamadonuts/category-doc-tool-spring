package dev.catdoc.domain;

import static dev.catdoc.domain.Validation.requireNonBlank;

import java.util.List;
import java.util.Optional;

/** 文書と関係の集まり。圏にあたる。 */
public record Category(String id, String name, List<CategoryObject> objects, List<Morphism> morphisms) {

    public Category {
        requireNonBlank(id, "id");
        requireNonBlank(name, "name");
        objects = List.copyOf(objects);
        morphisms = List.copyOf(morphisms);
    }

    public Optional<CategoryObject> object(String objectId) {
        return objects.stream().filter(o -> o.id().equals(objectId)).findFirst();
    }

    public Optional<Morphism> morphism(String morphismId) {
        return morphisms.stream().filter(m -> m.id().equals(morphismId)).findFirst();
    }

    public List<Morphism> morphismsFrom(String objectId) {
        return morphisms.stream().filter(m -> m.source().equals(objectId)).toList();
    }

    public List<Morphism> morphismsTo(String objectId) {
        return morphisms.stream().filter(m -> m.target().equals(objectId)).toList();
    }
}
