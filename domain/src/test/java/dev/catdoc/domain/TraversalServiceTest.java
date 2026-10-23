package dev.catdoc.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 既存版の tests/domain/services/TraversalService.test.ts を移し、上限のケースを足したもの。 */
class TraversalServiceTest {

    /** A→B→C→D と、近道 A→C、B→D。E はどこともつながらない。恒等射も含める。 */
    private static final Category GRAPH = new Category("g", "G",
            List.of(obj("A"), obj("B"), obj("C"), obj("D"), obj("E")),
            List.of(
                    Morphism.identity("A"),
                    arrow("ab", "A", "B"),
                    arrow("bc", "B", "C"),
                    arrow("cd", "C", "D"),
                    arrow("ac", "A", "C"),
                    arrow("bd", "B", "D")));

    private static CategoryObject obj(String id) {
        return CategoryObject.of(id, id, "test");
    }

    private static Morphism arrow(String id, String source, String target) {
        return Morphism.of(id, id, source, target);
    }

    private static List<String> ids(List<Morphism> path) {
        return path.stream().map(Morphism::id).toList();
    }

    @Nested
    class 最短経路 {

        @Test
        void 隣り合う文書は射1本() {
            assertThat(ids(TraversalService.shortestPath(GRAPH, "A", "B"))).containsExactly("ab");
        }

        @Test
        void 経路が複数あれば最も短いものを返す() {
            assertThat(ids(TraversalService.shortestPath(GRAPH, "A", "D"))).containsExactly("ab", "bd");
        }

        @ParameterizedTest(name = "{0} から {1}")
        @CsvSource({"A, A", "D, A", "A, E", "X, A", "A, Y"})
        void 経路がなければ空(String from, String to) {
            assertThat(TraversalService.shortestPath(GRAPH, from, to)).isEmpty();
        }
    }

    @Nested
    class 全経路 {

        @Test
        void 短い順に並ぶ() {
            assertThat(TraversalService.allPaths(GRAPH, "A", "D")).map(TraversalServiceTest::ids)
                    .containsExactly(List.of("ab", "bd"), List.of("ac", "cd"), List.of("ab", "bc", "cd"));
        }

        @Test
        void 恒等射と循環はたどらない() {
            var cyclic = new Category("c", "C", List.of(obj("A"), obj("B"), obj("C")),
                    List.of(Morphism.identity("A"), arrow("ab", "A", "B"), arrow("ba", "B", "A"), arrow("bc", "B", "C")));

            assertThat(TraversalService.allPaths(cyclic, "A", "C")).map(TraversalServiceTest::ids)
                    .containsExactly(List.of("ab", "bc"));
        }

        @ParameterizedTest(name = "{0} から {1}")
        @CsvSource({"A, A", "D, A", "A, E", "X, A"})
        void 経路がなければ空(String from, String to) {
            assertThat(TraversalService.allPaths(GRAPH, from, to)).isEmpty();
        }

        @Test
        void 深さ5を超える経路は返さない() {
            var objects = IntStream.rangeClosed(0, 6).mapToObj(i -> obj("N" + i)).toList();
            var morphisms = IntStream.range(0, 6).mapToObj(i -> arrow("e" + i, "N" + i, "N" + (i + 1))).toList();
            var chain = new Category("chain", "Chain", objects, morphisms);

            assertThat(TraversalService.allPaths(chain, "N0", "N5")).hasSize(1);
            assertThat(TraversalService.allPaths(chain, "N0", "N6")).isEmpty();
        }

        @Test
        void 最大20本で打ち切る() {
            var objects = new ArrayList<>(List.of(obj("A"), obj("Z")));
            var morphisms = new ArrayList<Morphism>();
            for (int i = 1; i <= 25; i++) {
                objects.add(obj("M" + i));
                morphisms.add(arrow("a" + i, "A", "M" + i));
                morphisms.add(arrow("z" + i, "M" + i, "Z"));
            }
            var fan = new Category("fan", "Fan", objects, morphisms);

            var paths = TraversalService.allPaths(fan, "A", "Z");

            assertThat(paths).hasSize(TraversalService.MAX_PATHS);
            assertThat(ids(paths.getFirst())).containsExactly("a1", "z1");
        }
    }
}
