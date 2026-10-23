package dev.catdoc.domain;

import static dev.catdoc.domain.Validation.copyOrdered;
import static dev.catdoc.domain.Validation.requireNonBlank;

import java.util.Map;
import java.util.Optional;

/** 文書どうしの関係。圏の射にあたる。 */
public record Morphism(String id, String name, String source, String target, Map<String, Object> metadata) {

    public Morphism {
        requireNonBlank(id, "id");
        requireNonBlank(name, "name");
        requireNonBlank(source, "source");
        requireNonBlank(target, "target");
        metadata = copyOrdered(metadata);
    }

    public static Morphism of(String id, String name, String source, String target) {
        return new Morphism(id, name, source, target, Map.of());
    }

    public static Morphism identity(String objectId) {
        return of("id-" + objectId, "id_" + objectId, objectId, objectId);
    }

    /** 既存版と同じく、始点と終点が同じ射を恒等射とみなす。 */
    public boolean isIdentity() {
        return source.equals(target);
    }

    /** f の後に g をたどる合成 g∘f。f の終点と g の始点が違えば空。 */
    public static Optional<Morphism> compose(Morphism f, Morphism g) {
        if (!f.target.equals(g.source)) {
            return Optional.empty();
        }
        return Optional.of(of("composed-" + f.id + "-" + g.id, g.name + "∘" + f.name, f.source, g.target));
    }
}
