package dev.catdoc.domain;

import java.util.List;

/** 関手の列をたどった追跡の表。 */
public record Traceability(List<String> functorIds, List<Row> rows) {

    public Traceability {
        functorIds = List.copyOf(functorIds);
        rows = List.copyOf(rows);
    }

    /**
     * 元のカテゴリの文書1つ分の行。mappedObjectIds は関手を順にたどって届いた対応先で、
     * 対応が切れたところで終わる。最後の関手まで届けば complete。
     */
    public record Row(String objectId, List<String> mappedObjectIds, boolean complete) {

        public Row {
            mappedObjectIds = List.copyOf(mappedObjectIds);
        }
    }
}
