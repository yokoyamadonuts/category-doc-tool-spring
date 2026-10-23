package dev.catdoc.api.infrastructure.persistence;

import dev.catdoc.api.application.Project;
import dev.catdoc.api.application.ProjectGraph;
import dev.catdoc.api.application.ProjectRepository;
import dev.catdoc.api.application.ProjectSummary;
import dev.catdoc.domain.Category;
import dev.catdoc.domain.CategoryObject;
import dev.catdoc.domain.Functor;
import dev.catdoc.domain.Morphism;
import dev.catdoc.domain.NaturalTransformation;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * プロジェクトを7つの表に分けて保存し、丸ごと読み戻す。
 * 日時は UTC の DATETIME(6) として読み書きし、JVM やセッションのタイムゾーンに左右されないようにする。
 */
@Repository
class JdbcProjectRepository implements ProjectRepository {

    private static final TypeReference<LinkedHashMap<String, Object>> METADATA = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;
    private final JsonMapper json;

    JdbcProjectRepository(JdbcClient jdbc, JdbcTemplate batch, JsonMapper json) {
        this.jdbc = jdbc;
        this.batch = batch;
        this.json = json;
    }

    @Override
    public List<ProjectSummary> findAllSummaries() {
        return jdbc.sql("SELECT id, name, imported_at FROM projects ORDER BY imported_at DESC, id")
                .query((rs, n) -> new ProjectSummary(UUID.fromString(rs.getString("id")), rs.getString("name"),
                        toInstant(rs.getObject("imported_at", LocalDateTime.class))))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findById(UUID id) {
        var key = id.toString();
        var summary = jdbc.sql("SELECT name, imported_at FROM projects WHERE id = ?").param(key)
                .query((rs, n) -> new ProjectSummary(id, rs.getString("name"),
                        toInstant(rs.getObject("imported_at", LocalDateTime.class))))
                .optional();
        return summary.map(s -> new Project(s.id(), s.name(), s.importedAt(), loadGraph(key)));
    }

    @Override
    @Transactional
    public void save(Project project) {
        var key = project.id().toString();
        var graph = project.graph();
        jdbc.sql("INSERT INTO projects (id, name, imported_at) VALUES (?, ?, ?)")
                .params(key, project.name(), LocalDateTime.ofInstant(project.importedAt(), ZoneOffset.UTC))
                .update();

        var categories = new ArrayList<Object[]>();
        var objects = new ArrayList<Object[]>();
        var morphisms = new ArrayList<Object[]>();
        for (var category : indexed(graph.categories())) {
            var c = category.value();
            categories.add(new Object[] {key, c.id(), c.name(), category.position()});
            for (var object : indexed(c.objects())) {
                var o = object.value();
                objects.add(new Object[] {key, o.id(), c.id(), o.title(), o.domain(), write(o.metadata()),
                        o.content(), object.position()});
            }
            for (var morphism : indexed(c.morphisms())) {
                var m = morphism.value();
                morphisms.add(new Object[] {key, c.id(), m.id(), m.name(), m.source(), m.target(),
                        write(m.metadata()), morphism.position()});
            }
        }

        var functors = new ArrayList<Object[]>();
        var mappings = new ArrayList<Object[]>();
        for (var functor : indexed(graph.functors())) {
            var f = functor.value();
            functors.add(new Object[] {key, f.id(), f.name(), f.sourceCategory(), f.targetCategory(), functor.position()});
            addMappings(mappings, key, f.id(), "OBJECT", f.objectMapping());
            addMappings(mappings, key, f.id(), "MORPHISM", f.morphismMapping());
        }

        var naturalTransformations = new ArrayList<Object[]>();
        var components = new ArrayList<Object[]>();
        for (var nt : indexed(graph.naturalTransformations())) {
            var t = nt.value();
            naturalTransformations.add(new Object[] {key, t.id(), t.name(), t.sourceFunctor(), t.targetFunctor(),
                    nt.position()});
            int position = 0;
            for (var component : t.components().entrySet()) {
                components.add(new Object[] {key, t.id(), component.getKey(), component.getValue(), position++});
            }
        }

        // 外部キーの向きに合わせ、参照される表から入れる
        insert("INSERT INTO categories (project_id, id, name, position) VALUES (?, ?, ?, ?)", categories);
        insert("""
                INSERT INTO objects (project_id, id, category_id, title, domain, metadata, content, position)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", objects);
        insert("""
                INSERT INTO morphisms (project_id, category_id, id, name, source_id, target_id, metadata, position)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", morphisms);
        insert("""
                INSERT INTO functors (project_id, id, name, source_category_id, target_category_id, position)
                VALUES (?, ?, ?, ?, ?, ?)""", functors);
        insert("""
                INSERT INTO functor_mappings (project_id, functor_id, kind, source_id, target_id, position)
                VALUES (?, ?, ?, ?, ?, ?)""", mappings);
        insert("""
                INSERT INTO natural_transformations
                    (project_id, id, name, source_functor_id, target_functor_id, position)
                VALUES (?, ?, ?, ?, ?, ?)""", naturalTransformations);
        insert("""
                INSERT INTO natural_transformation_components
                    (project_id, natural_transformation_id, object_id, morphism_id, position)
                VALUES (?, ?, ?, ?, ?)""", components);
    }

    @Override
    @Transactional
    public boolean deleteById(UUID id) {
        // 配下の行は ON DELETE CASCADE で消える
        return jdbc.sql("DELETE FROM projects WHERE id = ?").param(id.toString()).update() > 0;
    }

    private ProjectGraph loadGraph(String key) {
        var objectsByCategory = groupBy(jdbc.sql("""
                        SELECT category_id, id, title, domain, metadata, content FROM objects
                        WHERE project_id = ? ORDER BY position""").param(key)
                .query((rs, n) -> Map.entry(rs.getString("category_id"), new CategoryObject(rs.getString("id"),
                        rs.getString("title"), rs.getString("domain"), read(rs.getString("metadata")),
                        rs.getString("content"))))
                .list());
        var morphismsByCategory = groupBy(jdbc.sql("""
                        SELECT category_id, id, name, source_id, target_id, metadata FROM morphisms
                        WHERE project_id = ? ORDER BY position""").param(key)
                .query((rs, n) -> Map.entry(rs.getString("category_id"), new Morphism(rs.getString("id"),
                        rs.getString("name"), rs.getString("source_id"), rs.getString("target_id"),
                        read(rs.getString("metadata")))))
                .list());
        var categories = jdbc.sql("SELECT id, name FROM categories WHERE project_id = ? ORDER BY position")
                .param(key)
                .query((rs, n) -> new Category(rs.getString("id"), rs.getString("name"),
                        objectsByCategory.getOrDefault(rs.getString("id"), List.of()),
                        morphismsByCategory.getOrDefault(rs.getString("id"), List.of())))
                .list();

        // 鍵は "OBJECT:F" や "MORPHISM:F"
        var mappings = new LinkedHashMap<String, Map<String, String>>();
        jdbc.sql("""
                        SELECT functor_id, kind, source_id, target_id FROM functor_mappings
                        WHERE project_id = ? ORDER BY position""").param(key)
                .query(rs -> {
                    mappings.computeIfAbsent(rs.getString("kind") + ":" + rs.getString("functor_id"),
                            k -> new LinkedHashMap<>()).put(rs.getString("source_id"), rs.getString("target_id"));
                });
        var functors = jdbc.sql("""
                        SELECT id, name, source_category_id, target_category_id FROM functors
                        WHERE project_id = ? ORDER BY position""").param(key)
                .query((rs, n) -> new Functor(rs.getString("id"), rs.getString("name"),
                        rs.getString("source_category_id"), rs.getString("target_category_id"),
                        mappings.getOrDefault("OBJECT:" + rs.getString("id"), Map.of()),
                        mappings.getOrDefault("MORPHISM:" + rs.getString("id"), Map.of())))
                .list();

        var componentsByNt = new LinkedHashMap<String, Map<String, String>>();
        jdbc.sql("""
                        SELECT natural_transformation_id, object_id, morphism_id FROM natural_transformation_components
                        WHERE project_id = ? ORDER BY position""").param(key)
                .query(rs -> {
                    componentsByNt.computeIfAbsent(rs.getString("natural_transformation_id"), k -> new LinkedHashMap<>())
                            .put(rs.getString("object_id"), rs.getString("morphism_id"));
                });
        var naturalTransformations = jdbc.sql("""
                        SELECT id, name, source_functor_id, target_functor_id FROM natural_transformations
                        WHERE project_id = ? ORDER BY position""").param(key)
                .query((rs, n) -> new NaturalTransformation(rs.getString("id"), rs.getString("name"),
                        rs.getString("source_functor_id"), rs.getString("target_functor_id"),
                        componentsByNt.getOrDefault(rs.getString("id"), Map.of())))
                .list();

        return new ProjectGraph(categories, functors, naturalTransformations);
    }

    private void insert(String sql, List<Object[]> rows) {
        if (!rows.isEmpty()) {
            batch.batchUpdate(sql, rows);
        }
    }

    private static void addMappings(List<Object[]> rows, String key, String functorId, String kind,
            Map<String, String> mapping) {
        int position = 0;
        for (var entry : mapping.entrySet()) {
            rows.add(new Object[] {key, functorId, kind, entry.getKey(), entry.getValue(), position++});
        }
    }

    private record Indexed<T>(T value, int position) {
    }

    private static <T> List<Indexed<T>> indexed(List<T> values) {
        var result = new ArrayList<Indexed<T>>(values.size());
        for (int i = 0; i < values.size(); i++) {
            result.add(new Indexed<>(values.get(i), i));
        }
        return result;
    }

    private static <T> Map<String, List<T>> groupBy(List<Map.Entry<String, T>> entries) {
        var result = new LinkedHashMap<String, List<T>>();
        for (var entry : entries) {
            result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
        }
        return result;
    }

    private String write(Map<String, Object> metadata) {
        return json.writeValueAsString(metadata);
    }

    private Map<String, Object> read(String metadata) {
        return json.readValue(metadata, METADATA);
    }

    private static Instant toInstant(LocalDateTime utc) {
        return utc.toInstant(ZoneOffset.UTC);
    }
}
