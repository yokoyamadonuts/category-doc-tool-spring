package dev.catdoc.domain;

import static dev.catdoc.domain.Validation.copyOrdered;
import static dev.catdoc.domain.Validation.requireNonBlank;

import java.util.Map;

/** 文書。圏の対象にあたる。content は Markdown の本文で、ないときは空文字。 */
public record CategoryObject(String id, String title, String domain, Map<String, Object> metadata, String content) {

    public CategoryObject {
        requireNonBlank(id, "id");
        requireNonBlank(title, "title");
        requireNonBlank(domain, "domain");
        metadata = copyOrdered(metadata);
        content = content == null ? "" : content;
    }

    public static CategoryObject of(String id, String title, String domain) {
        return new CategoryObject(id, title, domain, Map.of(), "");
    }
}
