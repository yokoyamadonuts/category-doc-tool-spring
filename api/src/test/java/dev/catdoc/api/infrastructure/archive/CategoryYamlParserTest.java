package dev.catdoc.api.infrastructure.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.catdoc.api.application.ImportRejectedException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CategoryYamlParserTest {

    @Test
    void カテゴリと関手と自然変換を読む() {
        var definition = CategoryYamlParser.parse("""
                categories:
                  - id: c
                    name: C
                    objects:
                      - id: a
                        title: A
                        domain: d
                        metadata:
                          version: "1.0"
                          tags: [x, y]
                    morphisms:
                      - id: id-a
                        name: id_a
                        source: a
                        target: a
                functors:
                  - id: F
                    name: F
                    sourceCategory: c
                    targetCategory: c
                    objectMapping:
                      a: a
                    morphismMapping:
                      id-a: id-a
                naturalTransformations:
                  - id: eta
                    name: η
                    sourceFunctor: F
                    targetFunctor: F
                    components:
                      a: id-a
                """);

        var category = definition.categories().getFirst();
        assertThat(category.id()).isEqualTo("c");
        assertThat(category.objects().getFirst().metadata())
                .containsExactly(Map.entry("version", "1.0"), Map.entry("tags", java.util.List.of("x", "y")));
        assertThat(category.morphisms().getFirst().source()).isEqualTo("a");
        assertThat(definition.functors().getFirst().objectMapping()).containsEntry("a", "a");
        assertThat(definition.naturalTransformations().getFirst().components()).containsEntry("a", "id-a");
    }

    @Test
    void 省略された一覧は空として読む() {
        var definition = CategoryYamlParser.parse("categories:\n  - id: c\n    name: C\n");

        assertThat(definition.categories().getFirst().objects()).isEmpty();
        assertThat(definition.functors()).isEmpty();
        assertThat(definition.naturalTransformations()).isEmpty();
    }

    @Test
    void 空のファイルは空のプロジェクトとして読む() {
        assertThat(CategoryYamlParser.parse("").categories()).isEmpty();
    }

    @Test
    void 形式の誤りは場所つきで拒む() {
        assertThatThrownBy(() -> CategoryYamlParser.parse("categories:\n  - id: c\n    objects: not-a-list\n"))
                .isInstanceOfSatisfying(ImportRejectedException.class, e -> assertThat(e.errors())
                        .singleElement().asString()
                        .startsWith("category.yaml: ")
                        .contains("line 3"));
    }

    @Test
    void YAMLとして読めなければ拒む() {
        assertThatThrownBy(() -> CategoryYamlParser.parse("categories: [\n"))
                .isInstanceOfSatisfying(ImportRejectedException.class,
                        e -> assertThat(e.errors()).singleElement().asString().startsWith("category.yaml: "));
    }
}
