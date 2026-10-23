package dev.catdoc.api.application;

import dev.catdoc.domain.Category;
import dev.catdoc.domain.Functor;
import dev.catdoc.domain.Morphism;
import dev.catdoc.domain.Traceability;
import dev.catdoc.domain.TraceabilityService;
import dev.catdoc.domain.TraversalService;
import dev.catdoc.domain.VerificationResult;
import dev.catdoc.domain.VerificationService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** 保存したプロジェクトを読み、検証、経路、追跡の表をドメインの関数で毎回計算する。 */
@Service
public class AnalysisService {

    private final ProjectQueryService projectQueryService;

    public AnalysisService(ProjectQueryService projectQueryService) {
        this.projectQueryService = projectQueryService;
    }

    public record VerificationEntry(String id, String name, VerificationResult result) {
    }

    public record VerificationReport(List<VerificationEntry> categories, List<VerificationEntry> functors,
            List<VerificationEntry> naturalTransformations) {

        public boolean isValid() {
            return java.util.stream.Stream.of(categories, functors, naturalTransformations)
                    .flatMap(List::stream)
                    .allMatch(e -> e.result().isValid());
        }
    }

    public VerificationReport verify(UUID projectId) {
        var graph = projectQueryService.getProject(projectId).graph();

        var categories = graph.categories().stream()
                .map(c -> new VerificationEntry(c.id(), c.name(), VerificationService.verifyCategory(c)))
                .toList();
        // 取り込みで参照を確かめているので、関手の元と先のカテゴリは必ずある
        var functors = graph.functors().stream()
                .map(f -> new VerificationEntry(f.id(), f.name(), VerificationService.verifyFunctor(f,
                        graph.category(f.sourceCategory()).orElseThrow(),
                        graph.category(f.targetCategory()).orElseThrow())))
                .toList();
        var naturalTransformations = new ArrayList<VerificationEntry>();
        for (var nt : graph.naturalTransformations()) {
            var f = graph.functor(nt.sourceFunctor()).orElseThrow();
            var g = graph.functor(nt.targetFunctor()).orElseThrow();
            var result = VerificationService.verifyNaturalTransformation(nt, f, g,
                    graph.category(f.sourceCategory()).orElseThrow(), graph.category(f.targetCategory()).orElseThrow());
            naturalTransformations.add(new VerificationEntry(nt.id(), nt.name(), result));
        }
        return new VerificationReport(categories, functors, naturalTransformations);
    }

    public record PathResult(String from, String to, String categoryId, List<Morphism> shortest,
            List<List<Morphism>> all) {
    }

    /** @throws InvalidRequestException 2つの文書が違うカテゴリにあるとき */
    public PathResult findPaths(UUID projectId, String from, String to) {
        var graph = projectQueryService.getProject(projectId).graph();
        var category = categoryOf(graph, from);
        if (!category.id().equals(categoryOf(graph, to).id())) {
            throw new InvalidRequestException("'%s' and '%s' are in different categories".formatted(from, to));
        }
        return new PathResult(from, to, category.id(), TraversalService.shortestPath(category, from, to),
                TraversalService.allPaths(category, from, to));
    }

    public record TraceabilityReport(List<Functor> functors, Category start, Traceability table) {
    }

    /** @throws InvalidRequestException 知らない関手があるか、関手の元と先がつながらないとき */
    public TraceabilityReport trace(UUID projectId, List<String> functorIds) {
        var graph = projectQueryService.getProject(projectId).graph();
        if (functorIds.isEmpty()) {
            throw new InvalidRequestException("At least one functor is required");
        }
        var functors = functorIds.stream()
                .map(id -> graph.functor(id)
                        .orElseThrow(() -> new InvalidRequestException("Unknown functor '%s'".formatted(id))))
                .toList();
        var start = graph.category(functors.getFirst().sourceCategory()).orElseThrow();
        try {
            return new TraceabilityReport(functors, start, TraceabilityService.trace(start, functors));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    private static Category categoryOf(ProjectGraph graph, String objectId) {
        return graph.categoryOf(objectId)
                .orElseThrow(() -> new NotFoundException("Object '%s' was not found".formatted(objectId)));
    }
}
