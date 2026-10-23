package dev.catdoc.api.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** プロジェクトの保存。実装は infrastructure に置く。 */
public interface ProjectRepository {

    /** 新しい順。同じ日時なら ID の順。 */
    List<ProjectSummary> findAllSummaries();

    /** カテゴリ、関手、自然変換を含めて読む。 */
    Optional<Project> findById(UUID id);

    /** 1つのトランザクションで保存する。 */
    void save(Project project);

    /** @return 消したら true、なければ false */
    boolean deleteById(UUID id);
}
