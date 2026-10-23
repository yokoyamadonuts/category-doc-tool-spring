# 0008 lint を oxlint にし、型の検査を TypeScript 7 だけで行う

2026-10-23 採用

## 状況

画面は TypeScript 7 で型を検査する。lint は当初 ESLint 10 と typescript-eslint の予定だったが、typescript-eslint は TypeScript の JavaScript の API を使うため、TypeScript 6 を並べて入れる必要があった。Vite の公式テンプレートは oxlint を標準にしている。

## 決定

lint を oxlint にし、TypeScript は 7 だけを入れる。正しさの規則は誤り、疑わしい書き方は警告にし、警告も CI で落とす。React の hooks の規則は有効にする。

## 理由

- TypeScript を2つの版で並べると、どちらで検査しているのかが分かりにくい
- oxlint は TypeScript の API に頼らないので、TypeScript 7 だけで済む
- Vite の標準に寄せると、テンプレートの更新に追いつきやすい

## 結果

- typescript-eslint の型情報を使う規則は使えない。型の誤りは `tsc` で、hooks の誤りは oxlint の規則で見る
- hooks の規則が効くことは、わざと違反を書いて落ちることで確かめた
- `openapi-typescript` は TypeScript 5 を求めるので、npx で別に動かす。0004 を参照

## 採らなかった案

- ESLint 10 と TypeScript 6 を並べる: 上のとおり
- Biome: 候補になるが、Vite の標準ではない
