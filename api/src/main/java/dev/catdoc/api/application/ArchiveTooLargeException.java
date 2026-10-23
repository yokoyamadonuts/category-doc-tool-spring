package dev.catdoc.api.application;

/** 展開した量やファイルの数が上限を超えた。413 にあたる。 */
public class ArchiveTooLargeException extends RuntimeException {

    public ArchiveTooLargeException(String message) {
        super(message);
    }
}
