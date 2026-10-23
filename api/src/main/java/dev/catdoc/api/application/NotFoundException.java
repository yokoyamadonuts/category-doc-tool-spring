package dev.catdoc.api.application;

/** プロジェクト、カテゴリ、文書などが見つからない。404 にあたる。 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
