# 0003 MySQL 9.7 LTS を使う

2026-10-23 採用

## 状況

MySQL の LTS は 8.4 と 9.7 がある。Flyway が動作を確かめた MySQL は 9.4 までで、9.7 で起動すると次の警告が出る。

```
Using MySQL 9.7 which is newer than the version Flyway has been verified with. The latest verified version of MySQL is 9.4.
```

## 決定

`mysql:9.7` を使う。警告は受け入れる。`latest` のような動く札は使わない。

## 理由

- 9.7 は LTS で、8.4 よりサポートが長い
- 使うのは表の作成と変更だけで、Flyway の機能のうち版に左右されやすいものは使っていない
- 移行は Testcontainers の MySQL 9.7 で毎回流しており、壊れればテストで分かる

## 結果

- Flyway の起動のたびに警告が出る
- Flyway で動かないことが分かれば、8.4 LTS に下げる。SQL は 8.4 でも動く範囲で書いている
- 既定の照合順序 `utf8mb4_0900_ai_ci` は大文字と小文字を区別しない。文書や射の ID の列は `utf8mb4_bin` にし、`A` と `a` が主キーで衝突しないようにした

## 採らなかった案

- MySQL 8.4 LTS: Flyway の確認の範囲に入るが、サポートが先に終わる
- PostgreSQL: 再帰の問い合わせや JSON の扱いは強いが、経路探索はドメインで行い、JSON 型も使わないので、その利点が活きない。0005 と 0006 を参照
