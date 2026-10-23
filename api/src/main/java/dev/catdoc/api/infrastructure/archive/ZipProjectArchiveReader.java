package dev.catdoc.api.infrastructure.archive;

import dev.catdoc.api.application.ImportRejectedException;
import dev.catdoc.api.application.importing.DocumentDefinition;
import dev.catdoc.api.application.importing.ProjectArchiveReader;
import dev.catdoc.api.application.importing.ProjectDefinition;
import java.io.InputStream;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

/** zip を読み、category.yaml と、front matter に id を持つ Markdown を定義にする。 */
@Component
class ZipProjectArchiveReader implements ProjectArchiveReader {

    private final ZipArchiveReader zipReader = new ZipArchiveReader(ArchiveLimits.DEFAULT);

    @Override
    public ProjectDefinition read(InputStream archive) {
        var contents = zipReader.read(archive);
        var definition = CategoryYamlParser.parse(contents.categoryYaml());

        var documents = new ArrayList<DocumentDefinition>();
        var errors = new ArrayList<String>();
        contents.markdownFiles().forEach((path, markdown) -> {
            try {
                var frontMatter = FrontMatter.parse(path, markdown);
                if (frontMatter.metadata().get("id") instanceof String id && !id.isBlank()) {
                    documents.add(new DocumentDefinition(path, id, frontMatter.body()));
                }
            } catch (ImportRejectedException e) {
                errors.addAll(e.errors());
            }
        });
        if (!errors.isEmpty()) {
            throw new ImportRejectedException(errors);
        }
        return new ProjectDefinition(definition.categories(), definition.functors(),
                definition.naturalTransformations(), documents);
    }
}
