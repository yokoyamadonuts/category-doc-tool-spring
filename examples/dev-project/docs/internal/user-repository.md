---
id: internal-user-repository
title: UserRepositoryモジュール仕様
domain: internal-spec
module: infrastructure/repositories
language: TypeScript
---

# UserRepository モジュール仕様

## 概要

本ドキュメントは、UserRepositoryの実装詳細を定義する。

## 対応する設計

- @design-user-service - ユーザーサービス設計を実装
- #implement-functor - 設計→内部仕様の対応関係

## モジュール構成

```
src/infrastructure/repositories/
├── UserRepository.ts        # メイン実装
├── UserRepository.test.ts   # ユニットテスト
├── UserMapper.ts            # Entity ↔ DB マッピング
└── index.ts                 # エクスポート
```

## インターフェース

```typescript
// src/domain/repositories/IUserRepository.ts
export interface IUserRepository {
  findById(id: UserId): Promise<User | null>;
  findByEmail(email: Email): Promise<User | null>;
  findAll(options?: FindAllOptions): Promise<User[]>;
  save(user: User): Promise<void>;
  delete(id: UserId): Promise<void>;
  exists(id: UserId): Promise<boolean>;
}

export interface FindAllOptions {
  page?: number;
  limit?: number;
  search?: string;
  status?: UserStatus;
}
```

## 実装

```typescript
// src/infrastructure/repositories/UserRepository.ts
import { Pool } from 'pg';
import { IUserRepository, FindAllOptions } from '@/domain/repositories';
import { User, UserId, Email } from '@/domain/entities';
import { UserMapper } from './UserMapper';

export class PostgresUserRepository implements IUserRepository {
  constructor(private pool: Pool) {}

  async findById(id: UserId): Promise<User | null> {
    const result = await this.pool.query(
      'SELECT * FROM users WHERE id = $1 AND deleted_at IS NULL',
      [id.value]
    );
    return result.rows[0] ? UserMapper.toDomain(result.rows[0]) : null;
  }

  async findByEmail(email: Email): Promise<User | null> {
    const result = await this.pool.query(
      'SELECT * FROM users WHERE email = $1 AND deleted_at IS NULL',
      [email.value]
    );
    return result.rows[0] ? UserMapper.toDomain(result.rows[0]) : null;
  }

  async save(user: User): Promise<void> {
    const row = UserMapper.toPersistence(user);
    await this.pool.query(
      `INSERT INTO users (id, email, name, password_hash, status, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       ON CONFLICT (id) DO UPDATE SET
         name = $3, status = $5, updated_at = $7`,
      [row.id, row.email, row.name, row.password_hash, row.status, row.created_at, row.updated_at]
    );
  }

  async delete(id: UserId): Promise<void> {
    await this.pool.query(
      'UPDATE users SET deleted_at = NOW() WHERE id = $1',
      [id.value]
    );
  }
}
```

## マッパー

```typescript
// src/infrastructure/repositories/UserMapper.ts
export class UserMapper {
  static toDomain(row: UserRow): User {
    return new User({
      id: new UserId(row.id),
      email: new Email(row.email),
      name: new UserName(row.name),
      status: row.status as UserStatus,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    });
  }

  static toPersistence(user: User): UserRow {
    return {
      id: user.id.value,
      email: user.email.value,
      name: user.name.value,
      status: user.status,
      created_at: user.createdAt,
      updated_at: user.updatedAt,
    };
  }
}
```

## テーブルスキーマ

```sql
CREATE TABLE users (
  id VARCHAR(36) PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) DEFAULT 'active',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  deleted_at TIMESTAMP NULL
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;
```

## 関連ドキュメント

- @design-user-service - サービス設計
- @internal-jwt-handler - JWTハンドラ（本モジュールを使用）
- @infra-database - データベースインフラ
