# 開発プロジェクト ドキュメント整合性管理

このディレクトリは、CatDocを使って開発プロジェクトのドキュメントを圏論の概念で整理し、
**ドキュメント層間の整合性を関手（Functor）で担保する**例です。

## コンセプト

開発プロジェクトでは、要件定義から実装、デプロイまで複数の層のドキュメントが存在します。
これらの整合性を保つことは困難ですが、圏論の関手を使うことで整合性を構造的に管理できます。

```
Requirements ──[SpecifyFunctor]──> Specifications
（要件定義）                          （仕様書）
                                        │
                                   [DesignFunctor]
                                        ↓
                                     Design
                                    （設計書）
                                        │
                                  [ImplementFunctor]
                                        ↓
                                   InternalSpec
                                  （内部仕様書）
                                        │
                                   [DeployFunctor]
                                        ↓
                                  Infrastructure
                                 （インフラストラクチャ）
```

## 5つの圏（カテゴリ）

| 圏 | 説明 | ドメイン |
|---|------|---------|
| Requirements | ビジネス要件定義 | requirements |
| Specifications | API仕様・機能仕様 | specifications |
| Design | サービス設計・アーキテクチャ | design |
| InternalSpec | モジュール実装仕様 | internal-spec |
| Infrastructure | K8s・DBなどのインフラ | infrastructure |

## 4つの関手（Functor）

| 関手 | ソース | ターゲット | 目的 |
|-----|--------|-----------|------|
| SpecifyFunctor | Requirements | Specifications | 各要件が仕様でカバーされているか |
| DesignFunctor | Specifications | Design | 各仕様が設計に反映されているか |
| ImplementFunctor | Design | InternalSpec | 各設計が実装されているか |
| DeployFunctor | InternalSpec | Infrastructure | 各実装がデプロイされているか |

## ディレクトリ構造

```
dev-project/
├── category.yaml           # 圏と関手の定義
├── docs/
│   ├── requirements/       # 要件定義
│   │   ├── user-management.md
│   │   └── auth-system.md
│   ├── specs/              # 仕様書
│   │   ├── user-api-spec.md
│   │   └── auth-api-spec.md
│   ├── design/             # 設計書
│   │   ├── user-service-design.md
│   │   └── auth-service-design.md
│   ├── internal/           # 内部仕様書
│   │   ├── user-repository.md
│   │   └── jwt-handler.md
│   └── infrastructure/     # インフラストラクチャ
│       ├── kubernetes.md
│       └── database.md
└── README.md
```

## 使い方

### 1. ドキュメントをインポート

```bash
cd examples/dev-project

# 各層のドキュメントをインポート
catdoc import docs/requirements/*.md --domain requirements
catdoc import docs/specs/*.md --domain specifications
catdoc import docs/design/*.md --domain design
catdoc import docs/internal/*.md --domain internal-spec
catdoc import docs/infrastructure/*.md --domain infrastructure
```

### 2. 整合性を検証

```bash
# 全体の検証
catdoc validate

# 関手の検証（要件→仕様の対応をチェック）
catdoc validate --functors
```

### 3. ドキュメント間のトレースを確認

```bash
# 要件から仕様への対応を確認
catdoc trace req-user-management spec-user-api

# 要件からインフラまでの全パスを確認
catdoc trace req-user-management infra-k8s-user --all
```

### 4. ダッシュボードで可視化

```bash
catdoc dashboard
```

## 整合性検証の仕組み

関手は「構造を保存する写像」です。この特性を使って：

### 要件 → 仕様 の整合性

```yaml
# SpecifyFunctor
objectMapping:
  req-user-management: spec-user-api    # 要件 → 対応する仕様
  req-auth-system: spec-auth-api
morphismMapping:
  req-auth-depends-user: spec-auth-uses-user  # 依存関係も保存
```

すべての要件が仕様にマッピングされていれば、**要件のカバレッジは100%**。

### 仕様 → 設計 の整合性

```yaml
# DesignFunctor
objectMapping:
  spec-user-api: design-user-service
  spec-auth-api: design-auth-service
```

すべての仕様が設計にマッピングされていれば、**仕様の実現可能性が担保**される。

### 設計 → 実装 → インフラ

同様に、設計が実装に、実装がインフラにマッピングされていることで、
**設計通りに実装され、実装がデプロイされている**ことを検証できる。

## 実際のユースケース

### ユースケース1: 新機能追加時

1. 要件定義に新しいオブジェクトを追加
2. `catdoc validate` で「仕様が存在しない」警告を検出
3. 対応する仕様書を作成
4. 同様に設計書、内部仕様、インフラ設定を追加
5. 全層が揃ったら `catdoc validate` がパス

### ユースケース2: 設計変更時

1. 設計書のオブジェクトを変更
2. `catdoc validate` で内部仕様との不整合を検出
3. 内部仕様を更新
4. インフラ設定も必要に応じて更新

### ユースケース3: デプロイ前チェック

```bash
# すべての内部仕様がインフラにマッピングされているか確認
catdoc validate --functors | grep DeployFunctor
```

## 圏論の利点

1. **形式的な整合性**: 曖昧さのない対応関係
2. **自動検証**: ツールによる整合性チェック
3. **可視化**: グラフ構造としてドキュメント関係を把握
4. **トレーサビリティ**: 要件から実装まで追跡可能

## 関連ドキュメント

- [CatDoc README](../../README.md) - メインドキュメント
- [Math Docs Example](../math-docs/README.md) - 数学ドキュメントの例
