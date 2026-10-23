package dev.catdoc.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** record の生成時に使う検査と複製。 */
final class Validation {

    private Validation() {
    }

    static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be a non-empty string");
        }
        return value;
    }

    /** 順序を保ったまま複製する。YAML の値に null がありうるので Map.copyOf は使わない。 */
    static <V> Map<String, V> copyOrdered(Map<String, V> map) {
        return map == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
