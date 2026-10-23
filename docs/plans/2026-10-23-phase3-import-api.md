# 第3段階 取り込み、保存、API 実装計画

**Goal:** zip で文書群を取り込んで MySQL に保存し、グラフ、本文、検証、経路、追跡の表を API で返す。

**Architecture:** `api` の中を application、infrastructure、web に分ける。取り込みは infrastructure で zip と YAML を読み、application で構造を確かめてから1つのトランザクションで保存する。検証、経路、追跡は保存したものを読み、`domain` の関数で毎回計算する。

**Tech Stack:** Spring Boot 4.1.2、JdbcClient、Flyway、Jackson 3 の YAML、Testcontainers の MySQL 9.7、Playwright の API テスト。

## Global Constraints

- `openapi/openapi.yaml` を正本にする。先に仕様を書き、契約テストで実装と比べる
- エラーは RFC 9457 の Problem Details。422 は `errors` に文字列の配列を入れる
- 二重のテスト駆動: 外側は docker compose を相手にした Playwright の API テスト、内側は単体テストと Testcontainers の結合テスト

## この段階で決めたこと

| 項目 | 決めたこと | 理由 |
|---|---|---|
| 取り込みと検証の分担 | 取り込みは保存と参照に必要な構造だけを確かめる。圏論の公理は検証に任せる | 射の始点が存在しない、といった公理の違反は、画面で検証の結果として見せたい。取り込みで弾くと見せられない |
| 取り込みで弾くもの | YAML の形式違反、必須項目の欠け、ID の重複、存在しないカテゴリや関手への参照、front matter の `id` が文書にない Markdown | 保存できない、または API でたどれない |
| 文書の ID | プロジェクトの中で一意 | `GET /objects/{objectId}` で引けるように |
| 射の ID | カテゴリの中で一意 | 関手の射の対応はカテゴリの中の ID を指す |
| Markdown | front matter の `id` で文書に結びつけ、本文を保存する。`id` のないものは読み飛ばす | README.md などを含む zip をそのまま受け付けるため |
| zip の根 | `category.yaml` がちょうど1つあり、その場所を根とする | フォルダごと圧縮した zip も受け付ける |
| zip の上限 | 送信10MB、展開後の合計50MB、ファイル1,000個。展開した量は宣言値でなく実際に読んだ量で数える | zip 爆弾への備え |
| zip のパス | 絶対パス、`..`、`\` を含む名前を弾く | Zip Slip への備え。ディスクには書き出さないが、根の判定に使う |
| 保存の方法 | Spring Data JPA をやめて JdbcClient にする | グラフは表が7つで、まとめて入れて丸ごと読む。対応づけの手間に見合う利点がない |
| 追加の API | `GET /api/projects/{id}` と `DELETE /api/projects/{id}` | 取り込みの `Location` が指す先と、画面で選ぶカテゴリの一覧。削除は E2E の後始末にも使う |
| 自然変換の検証 | 成分の鍵は元のカテゴリ、成分の射は先のカテゴリから引く | 既存版は1つのカテゴリで両方を引いており、F と G がカテゴリをまたぐと正しく確かめられない |
| commonmark-java | 使わない | 本文は描画せずに保存するだけで、front matter は YAML として読める |

## API

| メソッドとパス | 成功 | 失敗 |
|---|---|---|
| `POST /api/projects` multipart の `file`、任意の `name` | 201 と `Location` | 400、413、422 |
| `GET /api/projects` | 200 | |
| `GET /api/projects/{id}` | 200 | 404 |
| `DELETE /api/projects/{id}` | 204 | 404 |
| `GET /api/projects/{id}/categories/{categoryId}/graph` | 200 | 404 |
| `GET /api/projects/{id}/objects/{objectId}` | 200 | 404 |
| `GET /api/projects/{id}/verification` | 200 | 404 |
| `GET /api/projects/{id}/paths?from=&to=` | 200 | 400、404 |
| `GET /api/projects/{id}/traceability?functors=F,G` | 200 | 400、404 |

経路は同じカテゴリの文書どうしで探す。カテゴリが違えば 400。

## 表

`V2__create_project_graph.sql` で作る。主キーは `project_id` から始め、プロジェクトごとの読み込みにそのまま使う。どの表も `projects` に `ON DELETE CASCADE` でつなぐ。並び順を保つため `position` を持つ。

```
categories(project_id, id, name, position)
objects(project_id, id, category_id, title, domain, metadata JSON, content MEDIUMTEXT, position)
morphisms(project_id, category_id, id, name, source_id, target_id, metadata JSON, position)
functors(project_id, id, name, source_category_id, target_category_id, position)
functor_mappings(project_id, functor_id, kind, source_id, target_id, position)
natural_transformations(project_id, id, name, source_functor_id, target_functor_id, position)
natural_transformation_components(project_id, natural_transformation_id, object_id, morphism_id, position)
```

## Tasks

### Task 1: 外側のループを開く

- [ ] `examples/` に既存版の例を置く
- [ ] `e2e/tests/import.spec.ts` を書く。dev-project を zip にして取り込み、検証が通ること、経路、追跡の表、文書の本文、グラフ、一覧への表示、削除を確かめる。壊れた zip は 422
- [ ] 一覧が空のときの E2E は、前に残ったプロジェクトを消してから確かめる
- [ ] 実行して FAIL を確かめる

### Task 2: 取り込み

- [ ] `ZipArchiveReader`: 上限、パスの検査、`category.yaml` の根の判定
- [ ] `CategoryYamlParser`: YAML を読み、形式の誤りを行ごとの文言にまとめる
- [ ] `FrontMatter`: Markdown を front matter と本文に分ける
- [ ] `ProjectAssembler`: 構造を確かめて domain のモデルにする。誤りはすべて集めてから `ImportRejectedException` にする
- [ ] それぞれ単体テストで FAIL を確かめてから実装する

### Task 3: 保存

- [ ] V2 のマイグレーション、`JdbcProjectRepository` の保存、読み込み、削除
- [ ] Testcontainers の結合テストで、dev-project を保存して読み戻すと同じモデルになることを確かめる

### Task 4: API

- [ ] `openapi.yaml` に全操作とスキーマを書く
- [ ] コントローラー、エラー処理、ユースケースを書き、WebMvcTest と結合テストで確かめる
- [ ] 契約テスト、E2E を通す
