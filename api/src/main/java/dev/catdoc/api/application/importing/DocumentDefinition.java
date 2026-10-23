package dev.catdoc.api.application.importing;

/** front matter に id を持つ Markdown。path は category.yaml からの相対パス。 */
public record DocumentDefinition(String path, String objectId, String body) {
}
