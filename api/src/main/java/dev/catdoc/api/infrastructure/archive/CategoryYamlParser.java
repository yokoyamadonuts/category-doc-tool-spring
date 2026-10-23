package dev.catdoc.api.infrastructure.archive;

import dev.catdoc.api.application.ImportRejectedException;
import dev.catdoc.api.application.importing.CategoryDefinition;
import dev.catdoc.api.application.importing.FunctorDefinition;
import dev.catdoc.api.application.importing.NaturalTransformationDefinition;
import dev.catdoc.api.application.importing.ProjectDefinition;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** category.yaml を定義として読む。項目の欠けや参照の誤りは確かめず、組み立てに任せる。 */
public final class CategoryYamlParser {

    private static final YAMLMapper YAML = YAMLMapper.builder().build();

    private record Document(List<CategoryDefinition> categories, List<FunctorDefinition> functors,
            List<NaturalTransformationDefinition> naturalTransformations) {
    }

    private CategoryYamlParser() {
    }

    public static ProjectDefinition parse(String yaml) {
        if (yaml.isBlank()) {
            return new ProjectDefinition(List.of(), List.of(), List.of(), List.of());
        }
        try {
            var document = YAML.readValue(yaml, Document.class);
            if (document == null) {
                return new ProjectDefinition(List.of(), List.of(), List.of(), List.of());
            }
            return new ProjectDefinition(document.categories(), document.functors(),
                    document.naturalTransformations(), List.of());
        } catch (JacksonException e) {
            var location = e.getLocation();
            var where = location == null ? "" : " (line %d, column %d)".formatted(location.getLineNr(), location.getColumnNr());
            throw new ImportRejectedException("category.yaml: " + e.getOriginalMessage() + where);
        }
    }
}
