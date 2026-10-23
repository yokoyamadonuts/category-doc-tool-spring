package dev.catdoc.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 既存版の tests/domain/services/VerificationService.test.ts を移したもの。 */
class VerificationServiceTest {

    private static CategoryObject obj(String id) {
        return CategoryObject.of(id, id, "test");
    }

    private static Morphism arrow(String id, String source, String target) {
        return Morphism.of(id, id, source, target);
    }

    @Nested
    class カテゴリ {

        @Test
        void 恒等射と合成がそろっていれば正しい() {
            var category = new Category("c", "C", List.of(obj("A"), obj("B"), obj("C")),
                    List.of(Morphism.identity("A"), Morphism.identity("B"), Morphism.identity("C"),
                            arrow("f", "A", "B"), arrow("g", "B", "C"), arrow("gf", "A", "C")));

            var result = VerificationService.verifyCategory(category);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        void 空のカテゴリは正しい() {
            assertThat(VerificationService.verifyCategory(new Category("e", "E", List.of(), List.of())).isValid())
                    .isTrue();
        }

        @Test
        void 恒等射のない文書は誤り() {
            var category = new Category("c", "C", List.of(obj("A"), obj("B")),
                    List.of(Morphism.identity("A"), arrow("f", "A", "B")));

            var result = VerificationService.verifyCategory(category);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).containsExactly("Object 'B' lacks an identity morphism (id: B → B)");
        }

        @Test
        void 存在しない文書を指す射は誤り() {
            var category = new Category("c", "C", List.of(obj("A")),
                    List.of(Morphism.identity("A"), arrow("f", "X", "A"), arrow("g", "A", "Y")));

            var result = VerificationService.verifyCategory(category);

            assertThat(result.errors()).containsExactly(
                    "Morphism 'f' has undefined source object 'X'",
                    "Morphism 'g' has undefined target object 'Y'");
        }

        @Test
        void 合成の欠けは警告にとどめる() {
            var category = new Category("c", "C", List.of(obj("A"), obj("B"), obj("C")),
                    List.of(Morphism.identity("A"), Morphism.identity("B"), Morphism.identity("C"),
                            arrow("f", "A", "B"), arrow("g", "B", "C")));

            var result = VerificationService.verifyCategory(category);

            assertThat(result.isValid()).isTrue();
            assertThat(result.warnings()).containsExactly("Missing composition: g∘f (A → C)");
        }
    }

    @Nested
    class 関手 {

        private final Category source = new Category("src", "Source", List.of(obj("A"), obj("B")),
                List.of(Morphism.identity("A"), Morphism.identity("B"), arrow("f", "A", "B")));
        private final Category target = new Category("tgt", "Target", List.of(obj("A'"), obj("B'")),
                List.of(Morphism.identity("A'"), Morphism.identity("B'"), arrow("f'", "A'", "B'")));

        @Test
        void 文書と恒等射の対応がそろっていれば正しい() {
            var functor = new Functor("F", "F", "src", "tgt", Map.of("A", "A'", "B", "B'"),
                    Map.of("id-A", "id-A'", "id-B", "id-B'", "f", "f'"));

            var result = VerificationService.verifyFunctor(functor, source, target);

            assertThat(result.errors()).isEmpty();
        }

        @Test
        void 対応のない文書は誤り() {
            var functor = new Functor("F", "F", "src", "tgt", Map.of("A", "A'"), Map.of("id-A", "id-A'"));

            var result = VerificationService.verifyFunctor(functor, source, target);

            assertThat(result.errors()).containsExactly("Object 'B' is not mapped by functor 'F'");
        }

        @Test
        void 存在しない文書への対応は誤り() {
            var functor = new Functor("F", "F", "src", "tgt", Map.of("A", "A'", "B", "NonExistent"), Map.of());

            var result = VerificationService.verifyFunctor(functor, source, target);

            assertThat(result.errors()).containsExactly("Functor 'F' maps 'B' to non-existent object 'NonExistent'");
        }

        @Test
        void 恒等射を恒等射でない射に移すと誤り() {
            var functor = new Functor("F", "F", "src", "tgt", Map.of("A", "A'", "B", "B'"),
                    Map.of("id-A", "f'", "id-B", "id-B'"));

            var result = VerificationService.verifyFunctor(functor, source, target);

            assertThat(result.errors()).containsExactly(
                    "Functor 'F' does not preserve identity for object 'A': F(id-A) = f' is not the identity on 'A''");
        }

        @Test
        void 恒等射を存在しない射に移すと誤り() {
            var functor = new Functor("F", "F", "src", "tgt", Map.of("A", "A'", "B", "B'"),
                    Map.of("id-A", "some-other-morphism", "id-B", "id-B'"));

            var result = VerificationService.verifyFunctor(functor, source, target);

            assertThat(result.errors()).containsExactly(
                    "Functor 'F' does not preserve identity for object 'A': F(id-A) = some-other-morphism is not the identity on 'A''");
        }

        @Test
        void 射の始点と終点を保たない対応は誤り() {
            // 既存版の multi-domain.yaml では、g: set-b → set-c を is-positive: type-int → type-bool に移していた
            var functor = new Functor("F", "F", "src", "tgt", Map.of("A", "B'", "B", "B'"),
                    Map.of("f", "f'"));

            var result = VerificationService.verifyFunctor(functor, source, target);

            assertThat(result.errors()).containsExactly(
                    "Functor 'F' does not preserve morphism 'f': F(f) = f' goes A' → B', expected B' → B'");
        }

        @Test
        void 対応のない射は警告にとどめる() {
            var functor = new Functor("F", "F", "src", "tgt", Map.of("A", "A'", "B", "B'"),
                    Map.of("id-A", "id-A'", "id-B", "id-B'"));

            var result = VerificationService.verifyFunctor(functor, source, target);

            assertThat(result.isValid()).isTrue();
            assertThat(result.warnings()).containsExactly("Morphism 'f' is not mapped by functor 'F'");
        }

        @Test
        void 存在しない射への対応は警告にとどめる() {
            var functor = new Functor("F", "F", "src", "tgt", Map.of("A", "A'", "B", "B'"),
                    Map.of("id-A", "id-A'", "id-B", "id-B'", "f", "ghost"));

            var result = VerificationService.verifyFunctor(functor, source, target);

            assertThat(result.isValid()).isTrue();
            assertThat(result.warnings())
                    .containsExactly("Functor 'F' maps morphism 'f' to non-existent morphism 'ghost'");
        }

        @Test
        void 恒等射のIDが命名規則に従わなくても保存を確かめる() {
            // 既存版は id-{対象} という命名を前提にしていたため、この誤りを見逃していた
            var src = new Category("src", "Source", List.of(obj("A")), List.of(arrow("loop-a", "A", "A")));
            var tgt = new Category("tgt", "Target", List.of(obj("A'"), obj("B'")),
                    List.of(arrow("loop-a2", "A'", "A'"), arrow("h", "A'", "B'")));
            var functor = new Functor("F", "F", "src", "tgt", Map.of("A", "A'"), Map.of("loop-a", "h"));

            var result = VerificationService.verifyFunctor(functor, src, tgt);

            assertThat(result.errors()).containsExactly(
                    "Functor 'F' does not preserve identity for object 'A': F(loop-a) = h is not the identity on 'A''");
        }
    }

    @Nested
    class 自然変換 {

        private final Category category = new Category("cat", "Cat", List.of(obj("A"), obj("B")),
                List.of(Morphism.identity("A"), Morphism.identity("B"), arrow("wrong", "B", "A")));
        private final Functor f = new Functor("F", "F", "cat", "cat", Map.of("A", "A", "B", "B"), Map.of());
        private final Functor g = new Functor("G", "G", "cat", "cat", Map.of("A", "A", "B", "B"), Map.of());

        @Test
        void 各文書の成分がFからGへの射なら正しい() {
            var eta = new NaturalTransformation("eta", "η", "F", "G", Map.of("A", "id-A", "B", "id-B"));

            assertThat(VerificationService.verifyNaturalTransformation(eta, f, g, category, category).isValid()).isTrue();
        }

        @Test
        void 成分の欠けは誤り() {
            var eta = new NaturalTransformation("eta", "η", "F", "G", Map.of("A", "id-A"));

            assertThat(VerificationService.verifyNaturalTransformation(eta, f, g, category, category).errors())
                    .containsExactly("Natural transformation 'eta' is missing component for object 'B'");
        }

        @Test
        void 存在しない成分は誤り() {
            var eta = new NaturalTransformation("eta", "η", "F", "G",
                    Map.of("A", "non-existent-morphism", "B", "id-B"));

            assertThat(VerificationService.verifyNaturalTransformation(eta, f, g, category, category).errors())
                    .containsExactly("Natural transformation 'eta' has non-existent component morphism "
                            + "'non-existent-morphism' for object 'A'");
        }

        @Test
        void 始点と終点の違う成分は誤り() {
            var eta = new NaturalTransformation("eta", "η", "F", "G", Map.of("A", "wrong", "B", "id-B"));

            assertThat(VerificationService.verifyNaturalTransformation(eta, f, g, category, category).errors())
                    .containsExactly("Component η_A has wrong source: expected 'A', got 'B'");
        }

        @Test
        void 成分の鍵は元のカテゴリ_成分の射は先のカテゴリから引く() {
            // 既存版の multi-domain.yaml と同じ形。F と G は sets から types への関手
            var sets = new Category("sets", "Sets", List.of(obj("set-a")), List.of(Morphism.identity("set-a")));
            var types = new Category("types", "Types", List.of(obj("type-int")),
                    List.of(Morphism.identity("type-int")));
            var typeFunctor = new Functor("F", "F", "sets", "types", Map.of("set-a", "type-int"), Map.of());
            var eta = new NaturalTransformation("eta", "η", "F", "F", Map.of("set-a", "id-type-int"));

            var result = VerificationService.verifyNaturalTransformation(eta, typeFunctor, typeFunctor, sets, types);

            assertThat(result.errors()).isEmpty();
        }

        @Test
        void 元と先のカテゴリが違う関手どうしは誤り() {
            var other = new Functor("H", "H", "cat", "elsewhere", Map.of("A", "A", "B", "B"), Map.of());
            var eta = new NaturalTransformation("eta", "η", "F", "H", Map.of("A", "id-A", "B", "id-B"));

            assertThat(VerificationService.verifyNaturalTransformation(eta, f, other, category, category).errors())
                    .containsExactly("Natural transformation 'eta' connects functors 'F' (cat → cat) "
                            + "and 'H' (cat → elsewhere) with different categories");
        }
    }
}
