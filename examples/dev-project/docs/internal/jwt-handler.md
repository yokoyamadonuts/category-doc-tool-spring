---
id: internal-jwt-handler
title: JWTHandlerモジュール仕様
domain: internal-spec
module: infrastructure/auth
language: TypeScript
---

# JWTHandler モジュール仕様

## 概要

本ドキュメントは、JWTトークン管理モジュールの実装詳細を定義する。

## 対応する設計

- @design-auth-service - 認証サービス設計を実装
- #implement-functor - 設計→内部仕様の対応関係

## 依存関係

- @internal-user-repository - ユーザーリポジトリを使用
- #internal-jwt-uses-user - ユーザー情報の取得に使用

## モジュール構成

```
src/infrastructure/auth/
├── JwtHandler.ts           # メイン実装
├── JwtHandler.test.ts      # ユニットテスト
├── TokenPayload.ts         # ペイロード型定義
├── JwtConfig.ts            # 設定
└── index.ts                # エクスポート
```

## インターフェース

```typescript
// src/domain/auth/ITokenManager.ts
export interface ITokenManager {
  generateAccessToken(user: User): string;
  generateRefreshToken(user: User): string;
  verifyAccessToken(token: string): TokenPayload;
  verifyRefreshToken(token: string): RefreshTokenPayload;
  revokeToken(jti: string): Promise<void>;
}

export interface TokenPayload {
  sub: string;       // userId
  email: string;
  roles: string[];
  jti: string;       // JWT ID
  iat: number;       // issued at
  exp: number;       // expiration
}

export interface RefreshTokenPayload {
  sub: string;
  type: 'refresh';
  jti: string;
  iat: number;
  exp: number;
}
```

## 実装

```typescript
// src/infrastructure/auth/JwtHandler.ts
import jwt from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { ITokenManager, TokenPayload, RefreshTokenPayload } from '@/domain/auth';
import { User } from '@/domain/entities';
import { JwtConfig } from './JwtConfig';

export class JwtHandler implements ITokenManager {
  constructor(
    private config: JwtConfig,
    private revokedTokenStore: IRevokedTokenStore
  ) {}

  generateAccessToken(user: User): string {
    const payload: TokenPayload = {
      sub: user.id.value,
      email: user.email.value,
      roles: user.roles,
      jti: uuidv4(),
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + this.config.accessTokenExpiry,
    };

    return jwt.sign(payload, this.config.privateKey, {
      algorithm: 'RS256',
    });
  }

  generateRefreshToken(user: User): string {
    const payload: RefreshTokenPayload = {
      sub: user.id.value,
      type: 'refresh',
      jti: uuidv4(),
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + this.config.refreshTokenExpiry,
    };

    return jwt.sign(payload, this.config.privateKey, {
      algorithm: 'RS256',
    });
  }

  verifyAccessToken(token: string): TokenPayload {
    const payload = jwt.verify(token, this.config.publicKey, {
      algorithms: ['RS256'],
    }) as TokenPayload;

    // 失効チェック
    if (this.revokedTokenStore.isRevoked(payload.jti)) {
      throw new TokenRevokedError();
    }

    return payload;
  }

  async revokeToken(jti: string): Promise<void> {
    await this.revokedTokenStore.add(jti);
  }
}
```

## 設定

```typescript
// src/infrastructure/auth/JwtConfig.ts
export interface JwtConfig {
  privateKey: string;
  publicKey: string;
  accessTokenExpiry: number;   // 秒（デフォルト: 3600 = 1時間）
  refreshTokenExpiry: number;  // 秒（デフォルト: 604800 = 7日）
  issuer: string;
}

export const defaultJwtConfig: Partial<JwtConfig> = {
  accessTokenExpiry: 3600,
  refreshTokenExpiry: 604800,
  issuer: 'auth-service',
};
```

## 失効トークンストア

```typescript
// src/infrastructure/auth/RevokedTokenStore.ts
export interface IRevokedTokenStore {
  add(jti: string, expiry?: number): Promise<void>;
  isRevoked(jti: string): Promise<boolean>;
}

export class RedisRevokedTokenStore implements IRevokedTokenStore {
  constructor(private redis: Redis) {}

  async add(jti: string, expiry: number = 604800): Promise<void> {
    await this.redis.setex(`revoked:${jti}`, expiry, '1');
  }

  async isRevoked(jti: string): Promise<boolean> {
    const result = await this.redis.get(`revoked:${jti}`);
    return result === '1';
  }
}
```

## エラーハンドリング

```typescript
export class TokenError extends Error {}
export class TokenExpiredError extends TokenError {}
export class TokenInvalidError extends TokenError {}
export class TokenRevokedError extends TokenError {}
```

## 関連ドキュメント

- @design-auth-service - サービス設計
- @internal-user-repository - ユーザーリポジトリ
- @infra-k8s-auth - Kubernetesデプロイ設定
