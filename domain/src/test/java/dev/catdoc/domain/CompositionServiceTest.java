package dev.catdoc.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** 既存版の tests/domain/services/CompositionService.test.ts を移したもの。 */
class CompositionServiceTest {

    @Test
    void 射の合成は元の射を記録する() {
        var f = Morphism.of("f", "f", "A", "B");
        var g = Morphism.of("g", "g", "B", "C");

        assertThat(CompositionService.composeMorphisms(f, g)).hasValueSatisfying(gf -> {
            assertThat(gf.source()).isEqualTo("A");
            assertThat(gf.target()).isEqualTo("C");
            assertThat(gf.metadata()).containsEntry("composedFrom", java.util.List.of("f", "g"));
        });
        assertThat(CompositionService.composeMorphisms(g, f)).isEmpty();
    }

    @Test
    void 三つの射を順に合成できる() {
        var f = Morphism.of("f", "f", "A", "B");
        var g = Morphism.of("g", "g", "B", "C");
        var h = Morphism.of("h", "h", "C", "D");

        var hgf = CompositionService.composeMorphisms(f, g).flatMap(gf -> CompositionService.composeMorphisms(gf, h));

        assertThat(hgf).hasValueSatisfying(m -> {
            assertThat(m.name()).isEqualTo("h∘g∘f");
            assertThat(m.source()).isEqualTo("A");
            assertThat(m.target()).isEqualTo("D");
        });
    }

    @Test
    void 関手の合成は対応をつなぐ() {
        var f = new Functor("F", "F", "C", "D", Map.of("A", "A'", "B", "B'"), Map.of("f", "f'", "g", "g'"));
        var g = new Functor("G", "G", "D", "E", Map.of("A'", "A''"), Map.of("f'", "f''"));

        assertThat(CompositionService.composeFunctors(f, g)).hasValueSatisfying(gf -> {
            assertThat(gf.id()).isEqualTo("composed-F-G");
            assertThat(gf.name()).isEqualTo("G∘F");
            assertThat(gf.sourceCategory()).isEqualTo("C");
            assertThat(gf.targetCategory()).isEqualTo("E");
            // G の対応がない B と g は合成後に落ちる
            assertThat(gf.objectMapping()).containsExactlyEntriesOf(Map.of("A", "A''"));
            assertThat(gf.morphismMapping()).containsExactlyEntriesOf(Map.of("f", "f''"));
        });
    }

    @Test
    void カテゴリがつながらない関手は合成できない() {
        var f = new Functor("F", "F", "C", "D", Map.of(), Map.of());
        var g = new Functor("G", "G", "X", "E", Map.of(), Map.of());

        assertThat(CompositionService.composeFunctors(f, g)).isEmpty();
    }
}
