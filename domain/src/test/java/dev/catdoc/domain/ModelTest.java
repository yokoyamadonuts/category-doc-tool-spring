package dev.catdoc.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** 既存版の tests/domain/{Object,Morphism,Category,Functor,NaturalTransformation}.test.ts を移したもの。 */
class ModelTest {

    static Stream<Arguments> 空文字を許さない項目() {
        return Stream.of(
                Arguments.of("id", (Consumer<String>) v -> CategoryObject.of(v, "t", "d")),
                Arguments.of("title", (Consumer<String>) v -> CategoryObject.of("a", v, "d")),
                Arguments.of("domain", (Consumer<String>) v -> CategoryObject.of("a", "t", v)),
                Arguments.of("id", (Consumer<String>) v -> Morphism.of(v, "f", "A", "B")),
                Arguments.of("name", (Consumer<String>) v -> Morphism.of("f", v, "A", "B")),
                Arguments.of("source", (Consumer<String>) v -> Morphism.of("f", "f", v, "B")),
                Arguments.of("target", (Consumer<String>) v -> Morphism.of("f", "f", "A", v)),
                Arguments.of("id", (Consumer<String>) v -> new Category(v, "C", List.of(), List.of())),
                Arguments.of("name", (Consumer<String>) v -> new Category("c", v, List.of(), List.of())),
                Arguments.of("id", (Consumer<String>) v -> new Functor(v, "F", "s", "t", Map.of(), Map.of())),
                Arguments.of("name", (Consumer<String>) v -> new Functor("F", v, "s", "t", Map.of(), Map.of())),
                Arguments.of("sourceCategory", (Consumer<String>) v -> new Functor("F", "F", v, "t", Map.of(), Map.of())),
                Arguments.of("targetCategory", (Consumer<String>) v -> new Functor("F", "F", "s", v, Map.of(), Map.of())),
                Arguments.of("id", (Consumer<String>) v -> new NaturalTransformation(v, "η", "F", "G", Map.of())),
                Arguments.of("name", (Consumer<String>) v -> new NaturalTransformation("eta", v, "F", "G", Map.of())),
                Arguments.of("sourceFunctor", (Consumer<String>) v -> new NaturalTransformation("eta", "η", v, "G", Map.of())),
                Arguments.of("targetFunctor", (Consumer<String>) v -> new NaturalTransformation("eta", "η", "F", v, Map.of())))
                .flatMap(a -> Stream.of("", "   ", null).map(blank -> Arguments.of(a.get()[0], a.get()[1], blank)));
    }

    @ParameterizedTest(name = "{0} が「{2}」なら例外")
    @MethodSource("空文字を許さない項目")
    void 必須の項目が空なら作れない(String field, Consumer<String> create, String blank) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create.accept(blank))
                .withMessage(field + " must be a non-empty string");
    }

    @Nested
    class 文書 {

        @Test
        void メタデータは複製され元の変更が及ばない() {
            var metadata = new HashMap<String, Object>(Map.of("status", "approved"));
            var object = new CategoryObject("a", "A", "d", metadata, "本文");
            metadata.put("status", "draft");

            assertThat(object.metadata()).containsEntry("status", "approved");
            assertThatThrownBy(() -> object.metadata().put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void 本文とメタデータは省略できる() {
            var object = CategoryObject.of("a", "A", "d");

            assertThat(object.metadata()).isEmpty();
            assertThat(object.content()).isEmpty();
        }
    }

    @Nested
    class 射 {

        private final Morphism f = Morphism.of("f", "f", "A", "B");
        private final Morphism g = Morphism.of("g", "g", "B", "C");

        @Test
        void 終点と始点が一致すれば合成できる() {
            assertThat(Morphism.compose(f, g)).hasValueSatisfying(gf -> {
                assertThat(gf.id()).isEqualTo("composed-f-g");
                assertThat(gf.name()).isEqualTo("g∘f");
                assertThat(gf.source()).isEqualTo("A");
                assertThat(gf.target()).isEqualTo("C");
            });
        }

        @Test
        void 終点と始点が一致しなければ合成できない() {
            assertThat(Morphism.compose(g, f)).isEmpty();
        }

        @Test
        void 恒等射は始点と終点が同じ() {
            var id = Morphism.identity("A");

            assertThat(id.id()).isEqualTo("id-A");
            assertThat(id.name()).isEqualTo("id_A");
            assertThat(id.isIdentity()).isTrue();
            assertThat(f.isIdentity()).isFalse();
        }

        @Test
        void 恒等射との合成は始点と終点を変えない() {
            assertThat(Morphism.compose(Morphism.identity("A"), f))
                    .hasValueSatisfying(m -> assertThat(List.of(m.source(), m.target())).containsExactly("A", "B"));
            assertThat(Morphism.compose(f, Morphism.identity("B")))
                    .hasValueSatisfying(m -> assertThat(List.of(m.source(), m.target())).containsExactly("A", "B"));
        }
    }

    @Nested
    class カテゴリ {

        private final Category category = new Category("c", "C",
                List.of(CategoryObject.of("A", "A", "d"), CategoryObject.of("B", "B", "d")),
                List.of(Morphism.of("f", "f", "A", "B"), Morphism.of("h", "h", "A", "B"), Morphism.identity("B")));

        @Test
        void 文書と射をIDで引ける() {
            assertThat(category.object("A")).map(CategoryObject::title).hasValue("A");
            assertThat(category.object("Z")).isEmpty();
            assertThat(category.morphism("f")).map(Morphism::target).hasValue("B");
            assertThat(category.morphism("z")).isEmpty();
        }

        @Test
        void 始点や終点で射を絞り込める() {
            assertThat(category.morphismsFrom("A")).extracting(Morphism::id).containsExactly("f", "h");
            assertThat(category.morphismsTo("B")).extracting(Morphism::id).containsExactly("f", "h", "id-B");
            assertThat(category.morphismsFrom("Z")).isEmpty();
        }

        @Test
        void 一覧は複製され元の変更が及ばない() {
            var objects = new ArrayList<>(List.of(CategoryObject.of("A", "A", "d")));
            var created = new Category("c", "C", objects, List.of());
            objects.add(CategoryObject.of("B", "B", "d"));

            assertThat(created.objects()).hasSize(1);
            assertThatThrownBy(() -> created.objects().clear()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class 関手と自然変換 {

        @Test
        void 関手は文書と射の対応を引ける() {
            var functor = new Functor("F", "F", "s", "t", Map.of("A", "A'"), Map.of("f", "f'"));

            assertThat(functor.mapObject("A")).hasValue("A'");
            assertThat(functor.mapObject("B")).isEmpty();
            assertThat(functor.mapMorphism("f")).hasValue("f'");
            assertThat(functor.mapMorphism("g")).isEmpty();
        }

        @Test
        void 自然変換は成分を引ける() {
            var eta = new NaturalTransformation("eta", "η", "F", "G", Map.of("A", "id-A"));

            assertThat(eta.component("A")).hasValue("id-A");
            assertThat(eta.component("B")).isEmpty();
        }

        @Test
        void 対応は複製され元の変更が及ばない() {
            var mapping = new HashMap<>(Map.of("A", "A'"));
            var functor = new Functor("F", "F", "s", "t", mapping, Map.of());
            var eta = new NaturalTransformation("eta", "η", "F", "G", mapping);
            mapping.put("B", "B'");

            assertThat(functor.objectMapping()).containsOnlyKeys("A");
            assertThat(eta.components()).containsOnlyKeys("A");
            assertThatThrownBy(() -> functor.objectMapping().put("C", "C'"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
