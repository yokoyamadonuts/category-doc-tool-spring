package dev.catdoc.api.application;

import java.util.List;

/** 取り込んだ内容の形式や参照に誤りがある。誤りはすべて集めて返す。422 にあたる。 */
public class ImportRejectedException extends RuntimeException {

    private final List<String> errors;

    public ImportRejectedException(List<String> errors) {
        super("Import rejected: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public ImportRejectedException(String error) {
        this(List.of(error));
    }

    public List<String> errors() {
        return errors;
    }
}
