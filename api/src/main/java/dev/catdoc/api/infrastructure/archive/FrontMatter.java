package dev.catdoc.api.infrastructure.archive;

import dev.catdoc.api.application.ImportRejectedException;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Markdown の先頭の --- で囲んだ YAML と、本文。 */
public record FrontMatter(Map<String, Object> metadata, String body) {

    private static final YAMLMapper YAML = YAMLMapper.builder().build();

    public static FrontMatter parse(String path, String markdown) {
        var lines = markdown.split("(?<=\n)");
        if (lines.length == 0 || !lines[0].strip().equals("---")) {
            return new FrontMatter(Map.of(), markdown);
        }
        int close = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].strip().equals("---")) {
                close = i;
                break;
            }
        }
        if (close < 0) {
            throw new ImportRejectedException("'%s': front matter is not closed with '---'".formatted(path));
        }
        var yaml = String.join("", java.util.Arrays.copyOfRange(lines, 1, close));
        var body = String.join("", java.util.Arrays.copyOfRange(lines, close + 1, lines.length));
        return new FrontMatter(readMetadata(path, yaml), body.replaceFirst("^(\\r?\\n)+", ""));
    }

    private static Map<String, Object> readMetadata(String path, String yaml) {
        if (yaml.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> metadata = YAML.readValue(yaml, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            return metadata == null ? Map.of() : metadata;
        } catch (JacksonException e) {
            throw new ImportRejectedException("'%s': invalid front matter: %s".formatted(path, e.getOriginalMessage()));
        }
    }
}
