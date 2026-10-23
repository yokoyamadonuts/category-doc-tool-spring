# CatDoc for Spring 設計書

2026年10月23日。[category-doc-tool](https://github.com/yokoyamadonuts/category-doc-tool) の TypeScript 版を、Spring Boot と React で書き直し、画面を足したもの。

## 目的

文書を圏論のオブジェクト、文書どうしの関係を射として扱い、文書群の構造を圏論の公理で検証する。要件から設計、設計からテストへの対応を関手で表し、対応の漏れを見つける。

既存版との違いは2つ。保存先を MySQL にしたことと、ブラウザで構造を見て検証できる画面を足したこと。

## 範囲

作るもの:

- 圏論の検証と経路探索、追跡の表を求めるドメイン
- API からの取り込み。`category.yaml` と Markdown をまとめた zip を受け取る
- REST API
- 画面4種: グラフの表示、検証の結果、経路の探索、追跡の表

作らないもの: CLI、MCP サーバー、画面からの取り込み、公開のデモ環境。

## 構成

docker compose で3つのコンテナを動かす。

| コンテナ | 中身 |
|---|---|
| web | React のビルド結果を nginx で配る。`/api` は api に中継する |
| api | Spring Boot。REST API だけを持つ |
| db | MySQL |

## 版

2026年10月23日時点の公式情報で確認した。

| 領域 | 版 | 判断 |
|---|---|---|
| Java | 25 LTS | 最新は 27 だが、Spring Framework が全面サポートするのは LTS だけ |
| Spring Boot / Framework | 4.1.2 / 7.0.10 | |
| 依存の版 | Spring Boot の BOM に従う | 上書きすると組み合わせの保証が外れる |
| ビルド | Gradle 9.8.2、Kotlin DSL | |
| DB | MySQL 9.7 LTS | イメージは `mysql:9.7`。Flyway で動かなければ 8.4 LTS に下げる |
| テスト | JUnit 6、AssertJ、Mockito、Testcontainers 2.0、ArchUnit 1.5 | |
| API の文書 | springdoc-openapi 3.1 | |
| 取り込み | Jackson 3 の YAML | front matter も YAML として読む。本文は描画しないので Markdown の解析器は使わない |
| 実行環境 | eclipse-temurin:25-jre-alpine | |
| TypeScript | 7.0 | |
| 画面 | React 19.3、Vite 8、React Router 8.4、TanStack Query 5 | React Router は 8.5.0 の公開から2週間たっていないので 8.4.0 にした |
| グラフ描画 | @xyflow/react 12、elkjs 0.12 | 配置と矢印の経路を ELK で求め、React Flow で描く。ELK は Web Worker で動かす |
| 検査とテスト | oxlint 1.89、Vitest 5、Playwright 1.64 | oxlint は Vite の公式テンプレートの標準。TypeScript の API を使わないので、TS 7 だけで済む。2026-10-23 に ESLint 10 から切り替えた |
| Node | 26 | 2026年10月28日に LTS になる |
| 配信 | nginx:1.30-alpine | |

## API

`openapi.yaml` を正本にする。

- 画面は `openapi-typescript` で型を生成し、`openapi-fetch` で呼ぶ
- サーバーのコントローラーは手で書く。springdoc の出力と `openapi.yaml` を CI で比べ、ずれたら落とす

1回の取り込みをプロジェクトと呼ぶ。プロジェクトの下に、カテゴリ、文書、関係、関手、自然変換を置く。

| メソッドとパス | 役割 |
|---|---|
| `POST /api/projects` | zip を取り込む。`201` と、相対の `Location` を返す |
| `GET /api/projects` | プロジェクトの一覧 |
| `GET /api/projects/{id}` | カテゴリ、関手、自然変換の一覧 |
| `DELETE /api/projects/{id}` | プロジェクトを消す |
| `GET /api/projects/{id}/categories/{categoryId}/graph` | カテゴリの文書と関係 |
| `GET /api/projects/{id}/objects/{objectId}` | 文書の本文 |
| `GET /api/projects/{id}/verification` | カテゴリ、関手、自然変換の検証の結果 |
| `GET /api/projects/{id}/paths?from=&to=` | 同じカテゴリの2つの文書の間の経路 |
| `GET /api/projects/{id}/traceability?functors=F,G` | 関手をつないだ追跡の表 |

エラーは RFC 9457 の Problem Details で返す。

| 状況 | 状態 |
|---|---|
| パラメータの誤り | 400 |
| 存在しない | 404 |
| zip が大きすぎる | 413 |
| YAML の形式違反、参照切れ | 422。誤りを `errors` 配列に並べる |
| 想定外 | 500。内部の情報は返さない |

## コードの層

Gradle のサブプロジェクトを2つ置く。

- `domain`: 依存のない素の Java。モデルは record で表し、検証、経路探索、追跡の表を純粋な関数で持つ
- `api`: Spring Boot。中を3層に分け、依存の向きを ArchUnit で検査する
  - `application`: ユースケースと、保存のためのインターフェース
  - `web`: コントローラーと DTO
  - `infrastructure`: JdbcClient による保存と、zip、YAML、Markdown の読み込み

画面は `web/src/features/` の下に、graph、verification、paths、traceability を置く。

## 取り込み

1. zip を受け取る。送信10MB、展開後の合計50MB、ファイル1,000個を上限にする。展開した量は実際に読んだ量で数える。絶対パス、`..`、`\` を含む名前は弾く
2. `category.yaml` がちょうど1つあり、その場所を根とする。front matter に `id` を持つ Markdown を、その文書の本文にする
3. ドメインのモデルを組み立て、保存と参照に要る構造を確かめる。必須の項目、ID の重複、存在しないカテゴリや関手への参照を弾く。圏論の公理の違反は弾かず、検証で見せる
4. 1つのトランザクションで保存する

## 検証と経路

検証の結果は保存せず、毎回計算する。既存版と同じく、次を確かめる。

- カテゴリ: 恒等射の存在、射の参照の整合性、合成の閉包
- 関手: 元と先のカテゴリの存在、文書の対応の完全性、対応先の存在、恒等射の保存、射の始点と終点の保存
- 自然変換: 可換性

経路は最短経路と、深さ5まで・最大20本の全経路を返す。

既存版から変えた点は3つ。関手の恒等射の保存は、ID の命名 `id-{文書}` に頼らず、始点と終点が同じ射を恒等射として確かめる。関手が射の始点と終点を保つかも確かめ、違えば誤り、射の対応が欠けていれば警告にする。全経路は DFS ではなく BFS で探し、上限で打ち切っても短い経路が残るようにする。詳しくは `docs/plans/2026-10-23-phase2-domain.md`。

## DB

表はプロジェクト、カテゴリ、文書、関係、関手、関手の対応、自然変換、自然変換の成分。主キーは `project_id` から始める。外部キーは各表から `projects` への1本だけにして `ON DELETE CASCADE` を付け、表どうしの参照は取り込みで確かめる。ID の列は大文字と小文字を区別する `utf8mb4_bin` にする。スキーマは Flyway で管理する。

## テスト

外側の E2E から書き、その内側でユニットテストを書いてから実装する、二重のテスト駆動で進める。

| 対象 | 方法 |
|---|---|
| E2E | docker compose で全体を立て、例のデータを取り込み、Playwright で画面を通す。機能ごとに最初に書く |
| ドメイン | 既存版のテストのケースを JUnit のパラメータ化テストに移す |
| 保存と API | Testcontainers の MySQL 9.7 で、HTTP から通しで確かめる |
| 仕様との一致 | springdoc の出力と `openapi.yaml` を比べる |
| 層の依存 | ArchUnit |
| 画面 | Vitest と Testing Library |

## CI と依存の更新

- GitHub Actions で、バックエンド、画面、仕様の一致、E2E を回す
- 依存の更新は Renovate で行い、公開から日の浅い版を入れない待ち期間を置く

## 判断の記録

`docs/adr/` に ADR を置く。一覧は [docs/adr/README.md](adr/README.md)。

## 進め方

1. 骨組み: リポジトリ、ビルド、docker compose、最初の E2E
2. ドメインとそのテスト
3. 取り込み、保存、API
4. 画面4種
5. CI、README、ADR
