---
id: design-auth-service
title: 認証サービス設計
domain: design
pattern: Clean Architecture
layer: Application
---

# 認証サービス設計

## 概要

本ドキュメントは、認証サービスのアーキテクチャ設計を定義する。

## 対応する仕様

- @spec-auth-api - 認証API仕様を実現
- #design-functor - 仕様→設計の対応関係

## 依存関係

- @design-user-service - ユーザーサービスを呼び出す
- #design-auth-calls-user - サービス間の依存関係

## アーキテクチャ

### コンポーネント構成

```
┌──────────────────────────────────────────────────────┐
│                  認証サービス                          │
├──────────────────────────────────────────────────────┤
│  AuthController                                      │
│    ├── LoginHandler                                  │
│    ├── LogoutHandler                                 │
│    └── RefreshHandler                                │
├──────────────────────────────────────────────────────┤
│  AuthService                                         │
│    ├── TokenManager                                  │
│    ├── SessionManager                                │
│    └── PasswordHasher                                │
├──────────────────────────────────────────────────────┤
│  UserServiceClient ─────────────> ユーザーサービス      │
└──────────────────────────────────────────────────────┘
```

## コンポーネント設計

### Application Layer

```typescript
class AuthService {
  constructor(
    private userService: IUserService,
    private tokenManager: ITokenManager,
    private sessionStore: ISessionStore
  ) {}

  async login(email: string, password: string): Promise<AuthResult> {
    // 1. ユーザー取得（UserServiceを呼び出し）
    // 2. パスワード検証
    // 3. トークン発行
    // 4. セッション作成
  }

  async logout(userId: string): Promise<void> {
    // セッション無効化
  }

  async refreshToken(refreshToken: string): Promise<TokenPair> {
    // トークン更新
  }
}
```

### Infrastructure Layer

```typescript
// トークン管理
class JwtTokenManager implements ITokenManager {
  generateAccessToken(user: User): string;
  generateRefreshToken(user: User): string;
  verifyToken(token: string): TokenPayload;
}

// セッション管理
class RedisSessionStore implements ISessionStore {
  create(session: Session): Promise<void>;
  get(sessionId: string): Promise<Session | null>;
  delete(sessionId: string): Promise<void>;
}
```

## シーケンス図

### ログインフロー

```
Client -> AuthController -> AuthService -> UserService -> Database
   |           |               |              |             |
   |  POST /auth/login         |              |             |
   |           |-------------->|              |             |
   |           |               |--getUser---->|             |
   |           |               |              |--SELECT---->|
   |           |               |<-------------|<------------|
   |           |               |              |             |
   |           |               |--verifyPassword            |
   |           |               |--generateTokens            |
   |           |               |--createSession------------>|
   |           |<--------------|              |             |
   |<----------|               |              |             |
```

## セキュリティ設計

### トークン署名

- アルゴリズム: RS256
- 鍵の管理: AWS KMS または HashiCorp Vault

### パスワードハッシュ

- アルゴリズム: bcrypt (cost=12)
- ソルト: 自動生成

### レート制限

- Redis ベースのスライディングウィンドウ方式

## 関連ドキュメント

- @spec-auth-api - API仕様
- @design-user-service - ユーザーサービス設計
- @internal-jwt-handler - 内部仕様（JWT実装）
