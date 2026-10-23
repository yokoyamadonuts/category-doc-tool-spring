package dev.catdoc.api.application.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.catdoc.api.application.ImportRejectedException;
import dev.catdoc.domain.Morphism;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectAssemblerTest {

    private static ObjectDefinition object(String id) {
        return new ObjectDefinition(id, id.toUpperCase(), "d", Map.of());
    }

    private static MorphismDefinition morphism(String id, String source, String target) {
        return new MorphismDefinition(id, id, source, target, Map.of());
    }

    private static CategoryDefinition category(String id, List<ObjectDefinition> objects,
            List<MorphismDefinition> morphisms) {
        return new CategoryDefinition(id, id.toUpperCase(), objects, morphisms);
    }

    private static ProjectDefinition project(List<CategoryDefinition> categories, List<FunctorDefinition> functors,
            List<NaturalTransformationDefinition> naturalTransformations, List<DocumentDefinition> documents) {
        return new ProjectDefinition(categories, functors, naturalTransformations, documents);
    }

    private static List<String> rejected(ProjectDefinition definition) {
        try {
            ProjectAssembler.assemble(definition);
        } catch (ImportRejectedException e) {
            return e.errors();
        }
        throw new AssertionError("取り込みが拒まれなかった");
    }

    @Test
    void 定義をドメインのモデルにしMarkdownの本文を文書に結びつける() {
        var definition = project(
                List.of(category("c", List.of(object("a"), object("b")), List.of(morphism("f", "a", "b")))),
                List.of(new FunctorDefinition("F", "F", "c", "c", Map.of("a", "a"), Map.of())),
                List.of(new NaturalTransformationDefinition("eta", "η", "F", "F", Map.of("a", "f"))),
                List.of(new DocumentDefinition("docs/a.md", "a", "# A\n")));

        var graph = ProjectAssembler.assemble(definition);

        var category = graph.categories().getFirst();
        assertThat(category.objects()).extracting(o -> o.id() + ":" + o.content()).containsExactly("a:# A\n", "b:");
        assertThat(category.morphisms()).extracting(Morphism::id).containsExactly("f");
        assertThat(graph.functors()).singleElement().satisfies(f -> assertThat(f.objectMapping()).containsEntry("a", "a"));
        assertThat(graph.naturalTransformations()).singleElement()
                .satisfies(nt -> assertThat(nt.component("a")).hasValue("f"));
    }

    @Test
    void 公理の違反は取り込みで弾かず検証に任せる() {
        var definition = project(
                List.of(category("c", List.of(object("a")), List.of(morphism("f", "a", "missing")))),
                List.of(new FunctorDefinition("F", "F", "c", "c", Map.of("a", "nowhere"), Map.of("f", "ghost"))),
                List.of(), List.of());

        assertThat(ProjectAssembler.assemble(definition).categories()).hasSize(1);
    }

    @Test
    void 必須の項目の欠けをすべて並べる() {
        var definition = project(
                List.of(
                        new CategoryDefinition(null, "C", List.of(), List.of()),
                        new CategoryDefinition("c", " ",
                                List.of(new ObjectDefinition("a", null, "d", Map.of())),
                                List.of(new MorphismDefinition("f", "f", null, "a", Map.of())))),
                List.of(new FunctorDefinition("F", "F", null, "c", Map.of(), Map.of())),
                List.of(new NaturalTransformationDefinition(null, "η", "F", "F", Map.of())),
                List.of());

        assertThat(rejected(definition)).containsExactly(
                "categories[0]: 'id' is required",
                "categories[1]: 'name' is required",
                "categories[1].objects[0]: 'title' is required",
                "categories[1].morphisms[0]: 'source' is required",
                "functors[0]: 'sourceCategory' is required",
                "naturalTransformations[0]: 'id' is required");
    }

    @Test
    void IDの重複を拒む() {
        var definition = project(
                List.of(
                        category("c", List.of(object("a")), List.of(morphism("f", "a", "a"), morphism("f", "a", "a"))),
                        category("c", List.of(object("a")), List.of())),
                List.of(new FunctorDefinition("F", "F", "c", "c", Map.of(), Map.of()),
                        new FunctorDefinition("F", "F", "c", "c", Map.of(), Map.of())),
                List.of(), List.of());

        assertThat(rejected(definition)).containsExactly(
                "Duplicate morphism id 'f' in category 'c'",
                "Duplicate category id 'c'",
                "Duplicate object id 'a'",
                "Duplicate functor id 'F'");
    }

    @Test
    void 存在しないカテゴリや関手への参照を拒む() {
        var definition = project(
                List.of(category("c", List.of(), List.of())),
                List.of(new FunctorDefinition("F", "F", "x", "y", Map.of(), Map.of())),
                List.of(new NaturalTransformationDefinition("eta", "η", "F", "G", Map.of())),
                List.of());

        assertThat(rejected(definition)).containsExactly(
                "Functor 'F' refers to unknown source category 'x'",
                "Functor 'F' refers to unknown target category 'y'",
                "Natural transformation 'eta' refers to unknown target functor 'G'");
    }

    @Test
    void 文書に結びつかないMarkdownを拒む() {
        var definition = project(
                List.of(category("c", List.of(object("a")), List.of())),
                List.of(), List.of(),
                List.of(
                        new DocumentDefinition("docs/a.md", "a", "# A"),
                        new DocumentDefinition("docs/a-copy.md", "a", "# A"),
                        new DocumentDefinition("docs/z.md", "z", "# Z")));

        assertThat(rejected(definition)).containsExactly(
                "'docs/a-copy.md' and 'docs/a.md' both refer to object 'a'",
                "'docs/z.md' refers to unknown object 'z'");
    }
}
