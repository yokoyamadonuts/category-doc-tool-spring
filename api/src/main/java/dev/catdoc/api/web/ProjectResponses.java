package dev.catdoc.api.web;

import dev.catdoc.api.application.AnalysisService;
import dev.catdoc.api.application.Project;
import dev.catdoc.api.application.ProjectQueryService;
import dev.catdoc.domain.Category;
import dev.catdoc.domain.Functor;
import dev.catdoc.domain.Morphism;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** openapi.yaml のスキーマに対応する応答。ドメインのモデルをそのまま外に出さない。 */
final class ProjectResponses {

    private ProjectResponses() {
    }

    record CategorySummary(String id, String name, int objectCount, int morphismCount) {
    }

    record FunctorSummary(String id, String name, String sourceCategory, String targetCategory) {

        static FunctorSummary from(Functor f) {
            return new FunctorSummary(f.id(), f.name(), f.sourceCategory(), f.targetCategory());
        }
    }

    record NaturalTransformationSummary(String id, String name, String sourceFunctor, String targetFunctor) {
    }

    record ProjectDetail(UUID id, String name, Instant importedAt, List<CategorySummary> categories,
            List<FunctorSummary> functors, List<NaturalTransformationSummary> naturalTransformations) {

        static ProjectDetail from(Project project) {
            var graph = project.graph();
            return new ProjectDetail(project.id(), project.name(), project.importedAt(),
                    graph.categories().stream()
                            .map(c -> new CategorySummary(c.id(), c.name(), c.objects().size(), c.morphisms().size()))
                            .toList(),
                    graph.functors().stream().map(FunctorSummary::from).toList(),
                    graph.naturalTransformations().stream()
                            .map(nt -> new NaturalTransformationSummary(nt.id(), nt.name(), nt.sourceFunctor(),
                                    nt.targetFunctor()))
                            .toList());
        }
    }

    record ObjectSummary(String id, String title, String domain) {
    }

    record MorphismView(String id, String name, String source, String target, boolean identity) {

        static MorphismView from(Morphism m) {
            return new MorphismView(m.id(), m.name(), m.source(), m.target(), m.isIdentity());
        }

        static List<MorphismView> from(List<Morphism> path) {
            return path.stream().map(MorphismView::from).toList();
        }
    }

    record CategoryGraph(String id, String name, List<ObjectSummary> objects, List<MorphismView> morphisms) {

        static CategoryGraph from(Category c) {
            return new CategoryGraph(c.id(), c.name(),
                    c.objects().stream().map(o -> new ObjectSummary(o.id(), o.title(), o.domain())).toList(),
                    MorphismView.from(c.morphisms()));
        }
    }

    record ObjectDetail(String id, String title, String domain, String categoryId, Map<String, Object> metadata,
            String content) {

        static ObjectDetail from(ProjectQueryService.ObjectInCategory found) {
            var o = found.object();
            return new ObjectDetail(o.id(), o.title(), o.domain(), found.categoryId(), o.metadata(), o.content());
        }
    }

    record VerificationEntry(String id, String name, boolean valid, List<String> errors, List<String> warnings) {

        static List<VerificationEntry> from(List<AnalysisService.VerificationEntry> entries) {
            return entries.stream()
                    .map(e -> new VerificationEntry(e.id(), e.name(), e.result().isValid(), e.result().errors(),
                            e.result().warnings()))
                    .toList();
        }
    }

    record VerificationReport(boolean valid, List<VerificationEntry> categories, List<VerificationEntry> functors,
            List<VerificationEntry> naturalTransformations) {

        static VerificationReport from(AnalysisService.VerificationReport report) {
            return new VerificationReport(report.isValid(), VerificationEntry.from(report.categories()),
                    VerificationEntry.from(report.functors()), VerificationEntry.from(report.naturalTransformations()));
        }
    }

    record PathResult(String from, String to, String categoryId, List<MorphismView> shortest,
            List<List<MorphismView>> all) {

        static PathResult from(AnalysisService.PathResult result) {
            return new PathResult(result.from(), result.to(), result.categoryId(), MorphismView.from(result.shortest()),
                    result.all().stream().map(MorphismView::from).toList());
        }
    }

    record TraceabilityRow(String objectId, String title, List<String> mappedObjectIds, boolean complete) {
    }

    record TraceabilityTable(List<FunctorSummary> functors, List<TraceabilityRow> rows) {

        static TraceabilityTable from(AnalysisService.TraceabilityReport report) {
            var start = report.start();
            return new TraceabilityTable(report.functors().stream().map(FunctorSummary::from).toList(),
                    report.table().rows().stream()
                            .map(r -> new TraceabilityRow(r.objectId(), start.object(r.objectId()).orElseThrow().title(),
                                    r.mappedObjectIds(), r.complete()))
                            .toList());
        }
    }
}
