package dev.catdoc.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 射と関手の合成。 */
public final class CompositionService {

    private CompositionService() {
    }

    /** g∘f を作り、metadata の composedFrom に元の射の ID を残す。 */
    public static Optional<Morphism> composeMorphisms(Morphism f, Morphism g) {
        return Morphism.compose(f, g).map(gf -> new Morphism(gf.id(), gf.name(), gf.source(), gf.target(),
                Map.of("composedFrom", List.of(f.id(), g.id()))));
    }

    /** G∘F を作る。F の対応先を G が移さないものは落とす。 */
    public static Optional<Functor> composeFunctors(Functor f, Functor g) {
        if (!f.targetCategory().equals(g.sourceCategory())) {
            return Optional.empty();
        }
        return Optional.of(new Functor("composed-" + f.id() + "-" + g.id(), g.name() + "∘" + f.name(),
                f.sourceCategory(), g.targetCategory(),
                chain(f.objectMapping(), g.objectMapping()), chain(f.morphismMapping(), g.morphismMapping())));
    }

    private static Map<String, String> chain(Map<String, String> first, Map<String, String> second) {
        var result = new LinkedHashMap<String, String>();
        first.forEach((from, via) -> {
            var to = second.get(via);
            if (to != null) {
                result.put(from, to);
            }
        });
        return result;
    }
}
