# 第2段階 ドメイン 実装計画

**Goal:** 圏、関手、自然変換のモデルと、検証、経路探索、追跡の表を、依存のない Java で書く。

**Architecture:** `domain` サブプロジェクトに record と、状態を持たない純粋な関数を置く。Spring にも DB にも依存しない。

**Tech Stack:** Java 25、JUnit 6、AssertJ。

## Global Constraints

- `domain` は外部ライブラリに依存しない。テストだけ JUnit と AssertJ を使う
- モデルは record で表し、コレクションは生成時に複製して変更できなくする
- 二重のテスト駆動: 外側は例のデータ dev-project を使う受け入れテスト、内側は record とサービスごとの単体テスト
- 検証の項目とエラーの文言は既存版に合わせる。既存版から変える点は下の「既存版との違い」に挙げる

## 既存版との違い

| 項目 | 既存版 | この版 | 理由 |
|---|---|---|---|
| 関手の恒等射の保存 | 恒等射の ID が `id-{対象}` であることを前提に探す | 始点と終点が同じ射を恒等射として探す | 例の dev-project は `id-req-user` のように命名が違い、既存版では確かめずに通っていた |
| 全経路の探索 | DFS で深さ10まで集めて長さで並べる | BFS で深さ5まで、短い順に最大20本 | 設計書の上限。BFS なら打ち切っても短い経路が残る |
| ドメインをまたぐ経路 | `findDomainPath` | 作らない | API にない。代わりに追跡の表を作る |
| 追跡の表 | なし | 関手の列をたどり、元のカテゴリの文書ごとに対応先を並べる | 設計書の `traceability` |

## ファイル構成

```
domain/src/main/java/dev/catdoc/domain/
  Validation.java              空文字の検査
  CategoryObject.java          文書
  Morphism.java                関係。compose と identity
  Category.java
  Functor.java
  NaturalTransformation.java
  VerificationResult.java
  VerificationService.java     カテゴリ、関手、自然変換の検証
  CompositionService.java      射と関手の合成
  TraversalService.java        最短経路と全経路
  Traceability.java            追跡の表と行
  TraceabilityService.java
domain/src/test/java/dev/catdoc/domain/
  DevProjectExample.java       例のデータを Java で組み立てる
  DevProjectAcceptanceTest.java 外側のループ
  各クラスの単体テスト
```

### Task 1: 外側のループを開く

- [ ] `DevProjectExample` と `DevProjectAcceptanceTest` を書く。確かめること:
  - 5つのカテゴリと4つの関手がすべて検証を通る
  - インフラで `infra-k8s-auth` から `infra-database` への最短経路は `infra-auth-uses-db`、全経路は2本で短い順
  - 4つの関手をつないだ追跡の表で、要件の2文書がインフラまで届く
  - 関手の対応を1つ抜くと、検証が誤りを返し、追跡の表のその行が途中で切れる
- [ ] `./gradlew :domain:test` でコンパイルが通らず FAIL することを確かめる

### Task 2: モデル

- [ ] 既存版の Object、Morphism、Category、Functor、NaturalTransformation のテストのうち、Java で意味のあるもの、つまり空文字の検査、検索、合成、恒等射の法則、複製による不変を、パラメータ化テストに移す
- [ ] FAIL を確かめてから record を書き、PASS を確かめる

### Task 3: 検証

- [ ] 既存版の VerificationService のテストを移す。恒等射の保存は、`id-` で始まらない恒等射でも確かめるケースを足す
- [ ] FAIL を確かめてから実装し、PASS を確かめる

### Task 4: 合成と経路

- [ ] CompositionService と TraversalService のテストを移す。全経路は深さ5と20本の上限、短い順を確かめるケースを足す
- [ ] FAIL を確かめてから実装し、PASS を確かめる

### Task 5: 追跡の表と、外側のループを閉じる

- [ ] 追跡の表の単体テストを書く。つながらない関手の列は `IllegalArgumentException`、対応が切れた行は `complete` が false
- [ ] FAIL を確かめてから実装し、受け入れテストを含めて PASS を確かめる
- [ ] コミット `feat: 圏論のドメインと検証、経路探索、追跡の表を追加する`
