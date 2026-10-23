# 判断の記録

設計で迷ったところと、採らなかった案を残す。書式は次のとおり。

- 状況: 何を決める必要があったか
- 決定: 何を選んだか
- 理由: なぜそれを選んだか
- 結果: 選んだことで得たものと、引き受けたもの
- 採らなかった案: 比べた案と、採らなかった理由

| 番号 | 題 | 状態 |
|---|---|---|
| [0001](0001-java-25-lts.md) | Java 25 LTS を使う | 採用 |
| [0002](0002-spring-boot-bom.md) | 依存の版を Spring Boot の BOM に従わせる | 採用 |
| [0003](0003-mysql-9-7-lts.md) | MySQL 9.7 LTS を使う | 採用 |
| [0004](0004-openapi-as-source-of-truth.md) | API は仕様書を正本にし、生成せずに比べる | 採用 |
| [0005](0005-path-search-in-domain.md) | 経路探索を DB ではなくドメインで行う | 採用 |
| [0006](0006-jdbc-client-over-jpa.md) | 保存を JPA ではなく JdbcClient で行う | 採用 |
| [0007](0007-import-checks-structure-only.md) | 取り込みは構造だけを確かめ、公理の違反は検証で見せる | 採用 |
| [0008](0008-oxlint-and-typescript-7.md) | lint を oxlint にし、型の検査を TypeScript 7 だけで行う | 採用 |
| [0009](0009-graph-layout-with-elk.md) | グラフの配置と矢印の経路を ELK で求める | 採用 |
