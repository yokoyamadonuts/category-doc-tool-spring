package dev.catdoc.domain;

import java.util.ArrayList;
import java.util.List;

/** 関手の列をつないで、元のカテゴリの文書が先のカテゴリのどの文書に届くかを表にする。 */
public final class TraceabilityService {

    private TraceabilityService() {
    }

    /**
     * start の文書ごとに、chain の関手を順に適用する。対応のない関手に当たったら、その行はそこで切れる。
     *
     * @throws IllegalArgumentException chain が空、または関手の元と先がつながらないとき
     */
    public static Traceability trace(Category start, List<Functor> chain) {
        requireChained(start, chain);

        var rows = new ArrayList<Traceability.Row>();
        for (var object : start.objects()) {
            var reached = new ArrayList<String>();
            var current = object.id();
            for (var functor : chain) {
                var next = functor.mapObject(current);
                if (next.isEmpty()) {
                    break;
                }
                current = next.get();
                reached.add(current);
            }
            rows.add(new Traceability.Row(object.id(), reached, reached.size() == chain.size()));
        }
        return new Traceability(chain.stream().map(Functor::id).toList(), rows);
    }

    private static void requireChained(Category start, List<Functor> chain) {
        if (chain.isEmpty()) {
            throw new IllegalArgumentException("At least one functor is required");
        }
        var first = chain.getFirst();
        if (!first.sourceCategory().equals(start.id())) {
            throw new IllegalArgumentException("Functor '%s' starts from '%s', not '%s'"
                    .formatted(first.id(), first.sourceCategory(), start.id()));
        }
        for (int i = 1; i < chain.size(); i++) {
            var previous = chain.get(i - 1);
            var next = chain.get(i);
            if (!previous.targetCategory().equals(next.sourceCategory())) {
                throw new IllegalArgumentException("Functors '%s' and '%s' cannot be chained: '%s' is not '%s'"
                        .formatted(previous.id(), next.id(), previous.targetCategory(), next.sourceCategory()));
            }
        }
    }
}
