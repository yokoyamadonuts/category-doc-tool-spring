package dev.catdoc.api.application.importing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Lists {

    private Lists() {
    }

    /** YAML で省略された一覧は空にする。要素の null は残し、組み立てで誤りにする。 */
    static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
    }

    static <V> Map<String, V> orEmpty(Map<String, V> map) {
        return map == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
