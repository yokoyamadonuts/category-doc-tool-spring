package dev.catdoc.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TraceabilityServiceTest {

    private final Category requirements = new Category("req", "Req",
            List.of(CategoryObject.of("r1", "r1", "req"), CategoryObject.of("r2", "r2", "req")), List.of());
    private final Functor f = new Functor("F", "F", "req", "spec", Map.of("r1", "s1", "r2", "s2"), Map.of());
    private final Functor g = new Functor("G", "G", "spec", "test", Map.of("s1", "t1"), Map.of());

    @Test
    void 元のカテゴリの文書ごとに対応先を並べる() {
        var table = TraceabilityService.trace(requirements, List.of(f, g));

        assertThat(table.functorIds()).containsExactly("F", "G");
        assertThat(table.rows()).containsExactly(
                new Traceability.Row("r1", List.of("s1", "t1"), true),
                new Traceability.Row("r2", List.of("s2"), false));
    }

    @Test
    void 最初の関手の元が渡したカテゴリでなければ例外() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TraceabilityService.trace(requirements, List.of(g)))
                .withMessage("Functor 'G' starts from 'spec', not 'req'");
    }

    @Test
    void つながらない関手の列は例外() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TraceabilityService.trace(requirements, List.of(f, f)))
                .withMessage("Functors 'F' and 'F' cannot be chained: 'spec' is not 'req'");
    }

    @Test
    void 関手が1つもなければ例外() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TraceabilityService.trace(requirements, List.of()))
                .withMessage("At least one functor is required");
    }
}
