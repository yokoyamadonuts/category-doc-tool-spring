package dev.catdoc.api.infrastructure.archive;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** zip から取り出した category.yaml と、その場所からの相対パスで引ける Markdown。 */
public record ArchiveContents(String categoryYaml, SortedMap<String, String> markdownFiles) {

    public ArchiveContents(String categoryYaml, Map<String, String> markdownFiles) {
        this(categoryYaml, new TreeMap<>(markdownFiles));
    }
}
