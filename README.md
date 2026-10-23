# CatDoc for Spring

文書を圏論のオブジェクト、文書どうしの関係を射として扱い、文書群の構造を圏論の公理で検証するツールです。[category-doc-tool](https://github.com/yokoyamadonuts/category-doc-tool) の TypeScript 版を、Spring Boot と React で書き直し、画面を足しています。

開発中です。設計は [docs/design.md](docs/design.md)、進め方は [docs/plans/](docs/plans/) にあります。

## 動かし方

Docker があれば、次のコマンドで画面、API、DB が立ち上がります。

```bash
docker compose up -d --build
```

- 画面: http://localhost:18080
- API: http://localhost:18081/api/projects

例のデータを取り込むには、`category.yaml` と Markdown をフォルダごと zip にして送ります。

```bash
cd examples && zip -qr /tmp/dev-project.zip dev-project
curl -i -F file=@/tmp/dev-project.zip http://localhost:18080/api/projects
```

返った `Location` の下で、検証の結果、経路、追跡の表を引けます。

```bash
curl http://localhost:18080/api/projects/{id}/verification
curl "http://localhost:18080/api/projects/{id}/paths?from=infra-k8s-auth&to=infra-database"
curl "http://localhost:18080/api/projects/{id}/traceability?functors=specify-functor,design-functor"
```

`category.yaml` の書き方は [examples/](examples/) を見てください。

## 開発

| 対象 | 必要なもの | テスト |
|---|---|---|
| API | Java 25、Docker | `./gradlew build` |
| 画面 | Node 26 | `cd web && npm ci && npm run lint && npm run typecheck && npm test` |
| E2E | Node 26、起動した docker compose | `cd e2e && npm ci && npx playwright install chromium && npm test` |

API の正本は [openapi/openapi.yaml](openapi/openapi.yaml) です。画面の型は `cd web && npm run gen:api` で生成します。

## 進め方

機能ごとに、E2E を先に書いて失敗を確かめ、その内側でユニットテストを書いてから実装します。
