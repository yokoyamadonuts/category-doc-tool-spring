---
id: spec-user-api
title: ユーザーAPI仕様
domain: specifications
version: "1.0"
format: OpenAPI
status: reviewed
---

# ユーザーAPI仕様

## 概要

本ドキュメントは、ユーザー管理APIの仕様を定義する。

## 対応する要件

- @req-user-management - ユーザー管理要件を実現
- #specify-functor - 要件→仕様の対応関係

## ベースURL

```
https://api.example.com/v1
```

## エンドポイント

### POST /users - ユーザー登録

**リクエスト**

```json
{
  "email": "user@example.com",
  "password": "SecurePass123",
  "name": "山田太郎"
}
```

**レスポンス（201 Created）**

```json
{
  "id": "usr_123456",
  "email": "user@example.com",
  "name": "山田太郎",
  "createdAt": "2024-01-15T10:00:00Z"
}
```

**エラーレスポンス**

| コード | 説明 |
|--------|------|
| 400 | バリデーションエラー |
| 409 | メールアドレス重複 |

### GET /users/:id - ユーザー取得

**パラメータ**

| 名前 | 型 | 説明 |
|------|-----|------|
| id | string | ユーザーID |

**レスポンス（200 OK）**

```json
{
  "id": "usr_123456",
  "email": "user@example.com",
  "name": "山田太郎",
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:00:00Z"
}
```

### PUT /users/:id - ユーザー更新

**リクエスト**

```json
{
  "name": "山田次郎"
}
```

**レスポンス（200 OK）**

更新後のユーザーオブジェクト

### DELETE /users/:id - ユーザー削除

**レスポンス（204 No Content）**

### GET /users - ユーザー一覧

**クエリパラメータ**

| 名前 | 型 | 説明 |
|------|-----|------|
| page | number | ページ番号（デフォルト: 1） |
| limit | number | 取得件数（デフォルト: 20） |
| search | string | 検索キーワード |

**レスポンス（200 OK）**

```json
{
  "users": [...],
  "total": 100,
  "page": 1,
  "limit": 20
}
```

## データモデル

### User

```typescript
interface User {
  id: string;
  email: string;
  name: string;
  status: "active" | "inactive" | "deleted";
  createdAt: string;
  updatedAt: string;
}
```

## 認証

すべてのエンドポイント（POST /users を除く）は認証が必要。

```
Authorization: Bearer <access_token>
```

## 関連ドキュメント

- @req-user-management - 要件定義
- @spec-auth-api - 認証API仕様（本APIを参照）
- @design-user-service - サービス設計
