package dev.catdoc.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** カテゴリの中で、文書から文書への射の経路を探す。恒等射はたどらない。 */
public final class TraversalService {

    /** 全経路の深さの上限。 */
    public static final int MAX_DEPTH = 5;
    /** 全経路の本数の上限。 */
    public static final int MAX_PATHS = 20;

    private TraversalService() {
    }

    private record Partial(String objectId, List<Morphism> path) {
    }

    /** 幅優先で最短の経路を1本返す。同じ文書どうしや、届かないときは空。 */
    public static List<Morphism> shortestPath(Category category, String from, String to) {
        if (!searchable(category, from, to)) {
            return List.of();
        }
        var visited = new HashSet<String>();
        visited.add(from);
        var queue = new ArrayDeque<Partial>();
        queue.add(new Partial(from, List.of()));
        while (!queue.isEmpty()) {
            var current = queue.poll();
            for (var m : outgoing(category, current.objectId())) {
                if (!visited.add(m.target())) {
                    continue;
                }
                var path = append(current.path(), m);
                if (m.target().equals(to)) {
                    return path;
                }
                queue.add(new Partial(m.target(), path));
            }
        }
        return List.of();
    }

    /** 深さ5まで、短い順に最大20本の経路を返す。 */
    public static List<List<Morphism>> allPaths(Category category, String from, String to) {
        return allPaths(category, from, to, MAX_DEPTH, MAX_PATHS);
    }

    /**
     * 経路を幅優先で伸ばすので、見つかる順がそのまま短い順になる。上限で打ち切っても短い経路が残る。
     * 1本の経路の中で同じ文書を2度通らない。
     */
    public static List<List<Morphism>> allPaths(Category category, String from, String to, int maxDepth,
            int maxPaths) {
        var found = new ArrayList<List<Morphism>>();
        if (!searchable(category, from, to)) {
            return found;
        }
        var queue = new ArrayDeque<Partial>();
        queue.add(new Partial(from, List.of()));
        while (!queue.isEmpty()) {
            var current = queue.poll();
            if (current.path().size() >= maxDepth) {
                continue;
            }
            for (var m : outgoing(category, current.objectId())) {
                if (m.target().equals(from) || current.path().stream().anyMatch(p -> p.target().equals(m.target()))) {
                    continue;
                }
                var path = append(current.path(), m);
                if (m.target().equals(to)) {
                    found.add(path);
                    if (found.size() >= maxPaths) {
                        return found;
                    }
                } else {
                    queue.add(new Partial(m.target(), path));
                }
            }
        }
        return found;
    }

    private static boolean searchable(Category category, String from, String to) {
        return !from.equals(to) && category.object(from).isPresent() && category.object(to).isPresent();
    }

    private static List<Morphism> outgoing(Category category, String objectId) {
        return category.morphismsFrom(objectId).stream().filter(m -> !m.isIdentity()).toList();
    }

    private static List<Morphism> append(List<Morphism> path, Morphism m) {
        var next = new ArrayList<>(path);
        next.add(m);
        return List.copyOf(next);
    }
}
