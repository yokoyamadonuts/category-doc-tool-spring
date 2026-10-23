# 第1段階 骨組みと最初の E2E 実装計画

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 画面、API、DB が一本通った最小の形を作る。画面はプロジェクトの一覧を表示し、ないときは「プロジェクトはまだありません」と出す。

**Architecture:** docker compose で web、api、db を動かす。web は nginx が React のビルド結果を配り、`/api` を api に中継する。api は Spring Boot で、`openapi.yaml` を正本にする。

**Tech Stack:** Java 25、Spring Boot 4.1.2、Gradle 9.8.2、MySQL 9.7、Flyway、Testcontainers 2.0、springdoc-openapi 3.1.1、React 19.3、Vite 8、TypeScript 7.0 と 6.0、ESLint 10、Vitest 5、openapi-typescript、openapi-fetch、Playwright 1.64、nginx 1.30。

## Global Constraints

- 版は docs/design.md の「版」の表に従う。Spring の依存は BOM に任せ、版を書かない。BOM にない springdoc だけ版を書く
- MySQL のイメージは `mysql:9.7`。`latest` は使わない
- 二重のテスト駆動: 機能ごとに E2E を先に書いて失敗を確かめ、内側のユニットテストを書いて失敗を確かめてから実装する
- エラーは RFC 9457 の Problem Details で返す
- 画面の文言は日本語。括弧書きを多用しない

## ファイル構成

```
settings.gradle.kts            ルート。domain と api を含める
build.gradle.kts               ルート。共通の設定はここに置かない
gradle/wrapper/                Gradle 9.8.2 の wrapper
domain/build.gradle.kts        依存のない java-library
api/build.gradle.kts           Spring Boot
api/src/main/java/dev/catdoc/api/
  ApiApplication.java
  application/ProjectSummary.java        一覧の1行
  application/ProjectQueryService.java   一覧を返すユースケース
  application/ProjectRepository.java     保存のためのインターフェース
  infrastructure/JpaProjectEntity.java
  infrastructure/SpringDataProjectJpaRepository.java
  infrastructure/JpaProjectRepository.java
  web/ProjectController.java
  web/ProjectSummaryResponse.java
api/src/main/resources/application.properties
api/src/main/resources/db/migration/V1__create_projects.sql
api/src/test/java/dev/catdoc/api/...     テスト
api/Dockerfile
openapi/openapi.yaml           API の正本
web/                           Vite + React
web/src/api/schema.d.ts        openapi.yaml から生成
web/src/api/client.ts
web/src/features/projects/ProjectList.tsx
web/src/features/projects/ProjectList.test.tsx
web/Dockerfile
web/nginx.conf
e2e/                           Playwright
e2e/tests/projects.spec.ts
compose.yaml
```

---

### Task 1: Gradle の骨組みと、DB につながる api

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat`
- Create: `domain/build.gradle.kts`
- Create: `api/build.gradle.kts`, `api/src/main/java/dev/catdoc/api/ApiApplication.java`, `api/src/main/resources/application.properties`
- Test: `api/src/test/java/dev/catdoc/api/TestcontainersConfiguration.java`, `api/src/test/java/dev/catdoc/api/ApiApplicationTests.java`

**Interfaces:**
- Produces: `TestcontainersConfiguration` を後のテストが `@Import` で使う

- [ ] **Step 1:** Spring Initializr で `type=gradle-project-kotlin`、`bootVersion=4.1.2`、`javaVersion=25`、`packageName=dev.catdoc.api`、依存は web、data-jpa、flyway、mysql、validation、actuator、testcontainers で雛形を作り、`api/` に置く。wrapper はルートに移す
- [ ] **Step 2:** ルートの `settings.gradle.kts`

```kotlin
rootProject.name = "category-doc-tool-spring"
include("domain", "api")
```

- [ ] **Step 3:** `domain/build.gradle.kts`

```kotlin
plugins { `java-library` }
java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
repositories { mavenCentral() }
dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform() }
```

- [ ] **Step 4:** `api/build.gradle.kts` に `implementation(project(":domain"))` と `implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.1.1")` を足す
- [ ] **Step 5:** `TestcontainersConfiguration` のイメージを `mysql:9.7` に変える
- [ ] **Step 6:** `./gradlew build` を実行する。期待: `contextLoads` が Testcontainers の MySQL 9.7 で通る
- [ ] **Step 7:** コミット `build: Gradle と Spring Boot の骨組みを作る`

### Task 2: 画面の骨組み

**Files:**
- Create: `web/` 一式（Vite の react-ts テンプレート）
- Modify: `web/package.json`、`web/eslint.config.js`、`web/vite.config.ts`

- [ ] **Step 1:** `npm create vite@latest web -- --template react-ts`
- [ ] **Step 2:** TypeScript を2つ並べる

```json
"devDependencies": {
  "typescript": "npm:@typescript/typescript6@^6.0.2",
  "@typescript/native": "npm:typescript@^7.0.2"
}
```

`tsc` は 7、`tsc6` は 6 を指す。`npm run typecheck` は `tsc --noEmit -p tsconfig.app.json` にする
- [ ] **Step 3:** Vitest 5、Testing Library、jsdom、openapi-typescript、openapi-fetch、TanStack Query 5 を入れる
- [ ] **Step 4:** `vite.config.ts` に、開発時の `/api` の中継と Vitest の設定を書く

```ts
export default defineConfig({
  plugins: [react()],
  server: { proxy: { '/api': 'http://localhost:8081' } },
  test: { environment: 'jsdom', setupFiles: ['./src/test/setup.ts'] },
})
```

- [ ] **Step 5:** `npm run lint && npm run typecheck && npm test` が通ることを確かめる
- [ ] **Step 6:** コミット `build: 画面の骨組みを作る`

### Task 3: docker compose と最初の E2E（外側のループを開く）

**Files:**
- Create: `compose.yaml`、`api/Dockerfile`、`web/Dockerfile`、`web/nginx.conf`
- Create: `e2e/package.json`、`e2e/playwright.config.ts`、`e2e/tests/projects.spec.ts`

- [ ] **Step 1:** 失敗する E2E を書く

```ts
import { test, expect } from '@playwright/test'

test('プロジェクトがないとき、一覧に空の案内が出る', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'CatDoc' })).toBeVisible()
  await expect(page.getByText('プロジェクトはまだありません')).toBeVisible()
})
```

- [ ] **Step 2:** `compose.yaml` と2つの Dockerfile、`nginx.conf` を書く。web は `8080`、api は `8081` で外に出す
- [ ] **Step 3:** `docker compose up -d --build` のあと `npx playwright test` を実行する。期待: 見出しも案内もないので FAIL
- [ ] **Step 4:** コミット `test: プロジェクト一覧の E2E を追加する`

### Task 4: プロジェクト一覧の API（内側のループ その1）

**Files:**
- Create: `openapi/openapi.yaml`
- Create: `api/src/main/resources/db/migration/V1__create_projects.sql`
- Create: `application/` と `infrastructure/` と `web/` の各ファイル
- Test: `api/src/test/java/dev/catdoc/api/web/ProjectControllerTest.java`、`api/src/test/java/dev/catdoc/api/ProjectApiIntegrationTest.java`

**Interfaces:**
- Produces: `GET /api/projects` が `[{ "id": string, "name": string, "importedAt": string }]` を返す

- [ ] **Step 1:** `openapi.yaml` に `GET /api/projects` と `ProjectSummary`、`Problem` のスキーマを書く
- [ ] **Step 2:** 失敗するテストを書く。コントローラーの単体テストは、ユースケースをモックして空の配列が返ることを確かめる。結合テストは、Testcontainers の MySQL で `GET /api/projects` が `200` と `[]` を返すことを確かめる
- [ ] **Step 3:** 実行して FAIL を確かめる
- [ ] **Step 4:** マイグレーション、エンティティ、リポジトリ、ユースケース、コントローラーを最小限で書く
- [ ] **Step 5:** 実行して PASS を確かめる
- [ ] **Step 6:** コミット `feat: プロジェクト一覧の API を追加する`

### Task 5: プロジェクト一覧の画面（内側のループ その2）と、外側のループを閉じる

**Files:**
- Create: `web/src/api/schema.d.ts`（生成）、`web/src/api/client.ts`、`web/src/features/projects/ProjectList.tsx`
- Modify: `web/src/App.tsx`
- Test: `web/src/features/projects/ProjectList.test.tsx`

- [ ] **Step 1:** `npx openapi-typescript ../openapi/openapi.yaml -o src/api/schema.d.ts`
- [ ] **Step 2:** 失敗するテストを書く。API が空の配列を返すとき「プロジェクトはまだありません」、1件返すときそのプロジェクト名が出ることを確かめる
- [ ] **Step 3:** 実行して FAIL を確かめる
- [ ] **Step 4:** `ProjectList` と `App` を書く。見出しは「CatDoc」
- [ ] **Step 5:** 実行して PASS を確かめる
- [ ] **Step 6:** `docker compose up -d --build` のあと E2E を実行する。期待: PASS
- [ ] **Step 7:** コミット `feat: プロジェクト一覧の画面を追加する`

### Task 6: 仕様書と実装の一致を確かめる

**Files:**
- Test: `api/src/test/java/dev/catdoc/api/OpenApiContractTest.java`

- [ ] **Step 1:** 失敗するテストを書く。springdoc の `/v3/api-docs` と `openapi/openapi.yaml` を読み、パスとメソッドの組、各操作の成功時の状態コードが一致することを確かめる。試しに `openapi.yaml` に存在しない操作を1つ書き、FAIL を確かめてから戻す
- [ ] **Step 2:** 実行して PASS を確かめる
- [ ] **Step 3:** コミット `test: 仕様書と実装の一致を確かめる契約テストを追加する`
