-- プロジェクトの ID は UUID なので ascii にする。外部キーで参照する列と文字コードと照合順序をそろえる
ALTER TABLE projects MODIFY id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;

-- 文書や射の ID は大文字と小文字を区別する。既定の utf8mb4_0900_ai_ci では A と a が主キーで衝突するため _bin にする。
-- metadata は JSON 型にしない。JSON 型はキーの順を並べ替えるので、YAML に書いた順を保てない。
-- 主キーは project_id から始め、プロジェクトごとの読み込みにそのまま使う。
-- 外部キーは projects への1本だけにし、プロジェクトの削除を配下に連鎖させる。表どうしの参照は取り込みで確かめる。
-- カテゴリから関手へ、のように連鎖の経路が複数あると、MySQL 9.7 では一部の行が消え残った。

CREATE TABLE categories (
    project_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id         VARCHAR(200) NOT NULL,
    name       VARCHAR(200) NOT NULL,
    position   INT          NOT NULL,
    PRIMARY KEY (project_id, id),
    CONSTRAINT fk_categories_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE objects (
    project_id  CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id          VARCHAR(200) NOT NULL,
    category_id VARCHAR(200) NOT NULL,
    title       VARCHAR(500) NOT NULL,
    domain      VARCHAR(200) NOT NULL,
    metadata    MEDIUMTEXT   NOT NULL,
    content     MEDIUMTEXT   NOT NULL,
    position    INT          NOT NULL,
    PRIMARY KEY (project_id, id),
    CONSTRAINT fk_objects_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE morphisms (
    project_id  CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    category_id VARCHAR(200) NOT NULL,
    id          VARCHAR(200) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    source_id   VARCHAR(200) NOT NULL,
    target_id   VARCHAR(200) NOT NULL,
    metadata    MEDIUMTEXT   NOT NULL,
    position    INT          NOT NULL,
    PRIMARY KEY (project_id, category_id, id),
    CONSTRAINT fk_morphisms_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE functors (
    project_id         CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id                 VARCHAR(200) NOT NULL,
    name               VARCHAR(200) NOT NULL,
    source_category_id VARCHAR(200) NOT NULL,
    target_category_id VARCHAR(200) NOT NULL,
    position           INT          NOT NULL,
    PRIMARY KEY (project_id, id),
    CONSTRAINT fk_functors_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE functor_mappings (
    project_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    functor_id VARCHAR(200) NOT NULL,
    kind       ENUM ('OBJECT', 'MORPHISM') NOT NULL,
    source_id  VARCHAR(200) NOT NULL,
    target_id  VARCHAR(200) NOT NULL,
    position   INT          NOT NULL,
    PRIMARY KEY (project_id, functor_id, kind, source_id),
    CONSTRAINT fk_functor_mappings_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE natural_transformations (
    project_id        CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id                VARCHAR(200) NOT NULL,
    name              VARCHAR(200) NOT NULL,
    source_functor_id VARCHAR(200) NOT NULL,
    target_functor_id VARCHAR(200) NOT NULL,
    position          INT          NOT NULL,
    PRIMARY KEY (project_id, id),
    CONSTRAINT fk_natural_transformations_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE natural_transformation_components (
    project_id                CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    natural_transformation_id VARCHAR(200) NOT NULL,
    object_id                 VARCHAR(200) NOT NULL,
    morphism_id               VARCHAR(200) NOT NULL,
    position                  INT          NOT NULL,
    PRIMARY KEY (project_id, natural_transformation_id, object_id),
    CONSTRAINT fk_natural_transformation_components_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;
