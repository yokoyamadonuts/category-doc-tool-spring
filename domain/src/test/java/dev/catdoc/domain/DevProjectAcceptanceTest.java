package dev.catdoc.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** 外側のループ。例のデータ dev-project で、検証、経路、追跡の表が通しで動くことを確かめる。 */
class DevProjectAcceptanceTest {

    private final Map<String, Category> categories = DevProjectExample.categories().stream()
            .collect(Collectors.toMap(Category::id, Function.identity()));

    @Test
    void すべてのカテゴリが圏の公理を満たす() {
        for (var category : categories.values()) {
            var result = VerificationService.verifyCategory(category);
            assertThat(result.errors()).as(category.id()).isEmpty();
            assertThat(result.warnings()).as(category.id()).isEmpty();
        }
    }

    @Test
    void すべての関手が恒等射を保つ() {
        for (var functor : DevProjectExample.functors()) {
            var result = VerificationService.verifyFunctor(functor,
                    categories.get(functor.sourceCategory()), categories.get(functor.targetCategory()));
            assertThat(result.errors()).as(functor.id()).isEmpty();
        }
    }

    @Test
    void インフラで認証からデータベースへの経路が短い順に見つかる() {
        var infrastructure = categories.get("infrastructure");

        assertThat(TraversalService.shortestPath(infrastructure, "infra-k8s-auth", "infra-database"))
                .extracting(Morphism::id)
                .containsExactly("infra-auth-uses-db");
        assertThat(TraversalService.allPaths(infrastructure, "infra-k8s-auth", "infra-database"))
                .map(path -> path.stream().map(Morphism::id).toList())
                .containsExactly(
                        List.of("infra-auth-uses-db"),
                        List.of("infra-auth-uses-user", "infra-user-uses-db"));
    }

    @Test
    void 要件の文書がインフラまで追跡できる() {
        var table = TraceabilityService.trace(categories.get("requirements"), DevProjectExample.functors());

        assertThat(table.functorIds())
                .containsExactly("specify-functor", "design-functor", "implement-functor", "deploy-functor");
        assertThat(table.rows()).containsExactly(
                new Traceability.Row("req-user-management",
                        List.of("spec-user-api", "design-user-service", "internal-user-repository", "infra-k8s-user"),
                        true),
                new Traceability.Row("req-auth-system",
                        List.of("spec-auth-api", "design-auth-service", "internal-jwt-handler", "infra-k8s-auth"),
                        true));
    }

    @Test
    void 設計から内部仕様への対応が抜けると検証で誤りになり追跡が途中で切れる() {
        var full = DevProjectExample.implement();
        var objectMapping = DevProjectExample.ordered("design-user-service", "internal-user-repository");
        var broken = new Functor(full.id(), full.name(), full.sourceCategory(), full.targetCategory(),
                objectMapping, full.morphismMapping());

        var result = VerificationService.verifyFunctor(broken, categories.get("design"), categories.get("internal-spec"));
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors())
                .contains("Object 'design-auth-service' is not mapped by functor 'implement-functor'");

        var chain = List.of(DevProjectExample.specify(), DevProjectExample.designFunctor(), broken,
                DevProjectExample.deploy());
        var table = TraceabilityService.trace(categories.get("requirements"), chain);
        assertThat(table.rows()).contains(
                new Traceability.Row("req-auth-system", List.of("spec-auth-api", "design-auth-service"), false));
    }
}
