# Math Docs Example

このディレクトリは、CatDocを使って数学のドキュメントを整理する例です。

## 構造

```
math-docs/
├── category.yaml    # カテゴリ構造の定義
├── docs/            # ドキュメント
│   ├── sets.md      # 集合論
│   ├── groups.md    # 群論
│   ├── rings.md     # 環論
│   └── fields.md    # 体論
└── README.md        # このファイル
```

## 使い方

### 1. プロジェクトを初期化（オプション）

```bash
cd examples/math-docs
catdoc init --force
```

### 2. ドキュメントをインポート

```bash
catdoc import docs/*.md --domain mathematics
```

### 3. 構造を検証

```bash
catdoc validate
```

### 4. オブジェクトを探索

```bash
# すべてのオブジェクトを一覧
catdoc list objects

# 特定のオブジェクトの詳細を表示
catdoc show object groups

# パスを探索（集合論から体論へ）
catdoc trace sets fields
```

### 5. ダッシュボードを起動

```bash
catdoc dashboard
```

## カテゴリ構造

この例では2つのカテゴリを定義しています：

### Algebra（代数学）

```
sets ──[add-operation]──> groups ──[add-multiplication]──> rings ──[add-inverses]──> fields
                                                             │
                                                             └──[scalar-action]──> modules
```

### Topology（位相空間論）

```
metric-spaces ──[induced-topology]──> topological-spaces
                                           ↑
manifolds ─────[underlying-space]──────────┘
```

## 圏論の概念

この例は以下の圏論の概念を示しています：

- **オブジェクト**: 数学的概念（集合、群、環など）
- **射**: 概念間の関係（構造を追加する操作）
- **恒等射**: 各オブジェクトの恒等変換
- **関手**: カテゴリ間の構造を保存する写像
