package dev.catdoc.api.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.catdoc.api.ExampleArchives;
import dev.catdoc.api.TestcontainersConfiguration;
import dev.catdoc.api.application.Project;
import dev.catdoc.api.application.ProjectGraph;
import dev.catdoc.api.application.ProjectRepository;
import dev.catdoc.api.application.importing.ProjectArchiveReader;
import dev.catdoc.api.application.importing.ProjectAssembler;
import dev.catdoc.domain.Category;
import dev.catdoc.domain.CategoryObject;
import dev.catdoc.domain.Morphism;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JdbcProjectRepositoryTest {

    @Autowired
    ProjectRepository repository;

    @Autowired
    ProjectArchiveReader archiveReader;

    @Autowired
    JdbcClient jdbc;

    private Project devProject() {
        var definition = archiveReader.read(new ByteArrayInputStream(ExampleArchives.zip("dev-project")));
        return new Project(UUID.randomUUID(), "dev-project", Instant.parse("2026-10-23T01:02:03.123456Z"),
                ProjectAssembler.assemble(definition));
    }

    @Test
    void 保存して読み戻すと同じモデルになる() {
        var project = devProject();

        repository.save(project);

        assertThat(repository.findById(project.id())).hasValue(project);
    }

    @Test
    void メタデータの順と型と大文字小文字を保つ() {
        var metadata = new java.util.LinkedHashMap<String, Object>();
        metadata.put("zeta", 1);
        metadata.put("alpha", List.of("x", true));
        metadata.put("nested", Map.of("k", "v"));
        metadata.put("empty", null);
        var graph = new ProjectGraph(
                List.of(new Category("c", "C",
                        List.of(new CategoryObject("A", "upper", "d", metadata, "本文"),
                                new CategoryObject("a", "lower", "d", Map.of(), "")),
                        List.of(Morphism.of("f", "f", "A", "a")))),
                List.of(), List.of());
        var project = new Project(UUID.randomUUID(), "case", Instant.parse("2026-10-23T00:00:00Z"), graph);

        repository.save(project);

        var loaded = repository.findById(project.id()).orElseThrow().graph().categories().getFirst();
        assertThat(loaded.objects()).extracting(CategoryObject::id).containsExactly("A", "a");
        assertThat(loaded.objects().getFirst().metadata()).containsExactlyEntriesOf(metadata);
    }

    @Test
    void 一覧は新しい順() {
        var older = new Project(UUID.randomUUID(), "older", Instant.parse("2026-10-22T00:00:00Z"),
                new ProjectGraph(List.of(), List.of(), List.of()));
        var newer = new Project(UUID.randomUUID(), "newer", Instant.parse("2026-10-23T00:00:00Z"),
                new ProjectGraph(List.of(), List.of(), List.of()));
        repository.save(older);
        repository.save(newer);

        assertThat(repository.findAllSummaries()).containsSubsequence(newer.summary(), older.summary());
    }

    @Test
    void 消すと配下の行もすべて消える() {
        var project = devProject();
        repository.save(project);

        assertThat(repository.deleteById(project.id())).isTrue();

        assertThat(repository.findById(project.id())).isEmpty();
        for (var table : List.of("categories", "objects", "morphisms", "functors", "functor_mappings")) {
            assertThat(jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE project_id = ?")
                    .param(project.id().toString()).query(Long.class).single()).as(table).isZero();
        }
    }

    @Test
    void ないものは消せない() {
        assertThat(repository.deleteById(UUID.randomUUID())).isFalse();
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }
}
