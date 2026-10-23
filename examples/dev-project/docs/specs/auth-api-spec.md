---
id: spec-auth-api
title: 認証API仕様
domain: specifications
version: "1.0"
format: OpenAPI
status: reviewed
---

# 認証API仕様

## 概要

本ドキュメントは、認証・認可APIの仕様を定義する。

## 対応する要件

- @req-auth-system - 認証システム要件を実現
- #specify-functor - 要件→仕様の対応関係

## 依存関係

- @spec-user-api - ユーザーAPI仕様を参照
- #spec-auth-uses-user - 認証はユーザー情報を必要とする

## ベースURL

```
https://api.example.com/v1
```

## エンドポイント

### POST /auth/login - ログイン

**リクエスト**

```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**レスポンス（200 OK）**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600,
  "tokenType": "Bearer",
  "user": {
    "id": "usr_123456",
    "email": "user@example.com",
    "name": "山田太郎"
  }
}
```

**エラーレスポンス**

| コード | 説明 |
|--------|------|
| 401 | 認証失敗 |
| 423 | アカウントロック |

### POST /auth/logout - ログアウト

**ヘッダー**

```
Authorization: Bearer <access_token>
```

**レスポンス（204 No Content）**

### POST /auth/refresh - トークン更新

**リクエスト**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**レスポンス（200 OK）**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600
}
```

### GET /auth/verify - トークン検証

**ヘッダー**

```
Authorization: Bearer <access_token>
```

**レスポンス（200 OK）**

```json
{
  "valid": true,
  "userId": "usr_123456",
  "expiresAt": "2024-01-15T11:00:00Z"
}
```

## トークン仕様

### アクセストークン

```json
{
  "sub": "usr_123456",
  "email": "user@example.com",
  "roles": ["user"],
  "iat": 1705312800,
  "exp": 1705316400
}
```

### リフレッシュトークン

```json
{
  "sub": "usr_123456",
  "type": "refresh",
  "iat": 1705312800,
  "exp": 1705917600
}
```

## エラーレスポンス形式

```json
{
  "error": {
    "code": "AUTH_FAILED",
    "message": "Invalid credentials"
  }
}
```

## レート制限

- ログイン: 10回/分
- その他: 60回/分

## 関連ドキュメント

- @req-auth-system - 要件定義
- @spec-user-api - ユーザーAPI仕様
- @design-auth-service - サービス設計
