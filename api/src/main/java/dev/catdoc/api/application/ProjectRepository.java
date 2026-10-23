package dev.catdoc.api.application;

import java.util.List;

/** プロジェクトを保存する先。実装は infrastructure 層に置く。 */
public interface ProjectRepository {

    /** 取り込んだ日時の新しい順に返す。 */
    List<ProjectSummary> findAllSummaries();
}
