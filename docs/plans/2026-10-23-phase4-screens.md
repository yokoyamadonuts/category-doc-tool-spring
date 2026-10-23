# 第4段階 画面4種 実装計画

**Goal:** 取り込んだプロジェクトを、グラフ、検証の結果、経路、追跡の表の4つの画面で見られるようにする。

**Architecture:** React Router のデータルーターで URL と画面を対応させ、データは TanStack Query で API から取る。選んだカテゴリや文書、経路の始点と終点、関手の列は URL の検索パラメータに持たせ、URL を共有すれば同じ画面が開けるようにする。グラフの配置は elkjs で計算し、@xyflow/react で描く。

**Tech Stack:** React 19.3、React Router 8.4、TanStack Query 5、@xyflow/react 12.12、elkjs 0.12、Vitest 5、Playwright 1.64。

## Global Constraints

- 公開から2週間たっていない版は入れない。React Router は 8.5.0 が 10月19日の公開なので 8.4.0 にする
- 画面の文言は日本語。括弧書きを多用しない
- 二重のテスト駆動: 外側は Playwright で画面を通す E2E、内側は Vitest と Testing Library
- グラフの描画は jsdom では確かめにくいので、配置の計算を純粋な関数に分けて単体テストし、描画は E2E で確かめる

## URL

| パス | 画面 | 検索パラメータ |
|---|---|---|
| `/` | プロジェクトの一覧 | |
| `/projects/:projectId/graph` | グラフ | `category`、`object` |
| `/projects/:projectId/verification` | 検証の結果 | |
| `/projects/:projectId/paths` | 経路 | `category`、`from`、`to` |
| `/projects/:projectId/traceability` | 追跡の表 | `functors` |

`/projects/:projectId` はグラフに移す。

## 画面

| 画面 | 内容 |
|---|---|
| 共通 | プロジェクト名の見出しと、グラフ、検証、経路、追跡のタブ |
| グラフ | カテゴリを選ぶと、文書を節点、関係を矢印で描く。恒等射は描かず、省いた本数を添える。節点を押すと、横に文書のメタデータと本文を出す |
| 検証 | 全体の判定と、カテゴリ、関手、自然変換ごとの誤りと警告 |
| 経路 | カテゴリ、始点、終点を選んで探す。最短経路と全経路を、文書と関係の並びで出す |
| 追跡 | 最初の関手を選び、続く関手は前の関手の先から始まるものだけを候補に出す。表は元の文書ごとに対応先を並べ、届かなかった欄は「対応なし」とする |

## Tasks

### Task 1: 外側のループを開く

- [ ] `e2e/tests/screens.spec.ts` を書く。dev-project を取り込み、一覧から開いてグラフ、文書の本文、検証、経路、追跡の表を通す。恒等射の欠けた小さなプロジェクトで、検証の誤りが出ることも確かめる
- [ ] 実行して FAIL を確かめる

### Task 2: ルーティングと共通の枠

- [ ] `routes.tsx` と、テスト用に任意の URL から描く `renderRoute`
- [ ] 一覧のリンク、プロジェクトの見出し、タブを、テストで FAIL を確かめてから作る

### Task 3: 検証、経路、追跡の画面

- [ ] それぞれ Testing Library のテストで FAIL を確かめてから作る
- [ ] 追跡の関手の候補を出す関数は純粋な関数にして単体テストする

### Task 4: グラフの画面と、外側のループを閉じる

- [ ] elkjs で配置を求める関数を単体テストしてから作る
- [ ] 文書の詳細の欄をテストしてから作る
- [ ] @xyflow/react でつなぎ、E2E を通す
