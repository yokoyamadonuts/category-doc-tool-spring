# 0002 依存の版を Spring Boot の BOM に従わせる

2026-10-23 採用

## 状況

Spring Boot の BOM は、Jackson、Flyway、Testcontainers、JUnit、MySQL の JDBC ドライバなどの版を、互いに動く組み合わせで決めている。個々の依存の版を自分で書くこともできる。

## 決定

`api` では、BOM が管理する依存の版を書かない。BOM にない springdoc-openapi だけ版を書く。

`domain` は Spring に依存しないので BOM を使わず、JUnit の BOM と AssertJ の版を書く。

## 理由

- 版を上書きすると、Spring が確かめた組み合わせから外れ、壊れたときに原因を切り分けにくい
- Spring Boot を上げれば依存の版もまとめて上がり、更新の作業が1か所で済む

## 結果

- 依存の新しい版がすぐには使えない。脆弱性の修正が急ぐときだけ、理由を書いて上書きする
- springdoc は Spring Boot との対応を自分で確かめる必要がある。3.1.1 が Boot 4.1 で動くことは契約テストで確かめている

## 採らなかった案

- すべての版を自分で書く: 最新を追えるが、組み合わせの確認を自分で負う
