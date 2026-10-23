package dev.catdoc.api.application.importing;

import java.io.InputStream;

/** zip を読んで定義にする。実装は infrastructure に置く。 */
public interface ProjectArchiveReader {

    /**
     * @throws dev.catdoc.api.application.ImportRejectedException 形式に誤りがあるとき
     * @throws dev.catdoc.api.application.ArchiveTooLargeException 上限を超えたとき
     */
    ProjectDefinition read(InputStream archive);
}
