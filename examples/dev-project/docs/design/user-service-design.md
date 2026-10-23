---
id: design-user-service
title: ユーザーサービス設計
domain: design
pattern: Clean Architecture
layer: Application
---

# ユーザーサービス設計

## 概要

本ドキュメントは、ユーザーサービスのアーキテクチャ設計を定義する。

## 対応する仕様

- @spec-user-api - ユーザーAPI仕様を実現
- #design-functor - 仕様→設計の対応関係

## アーキテクチャ

### レイヤー構成（Clean Architecture）

```
┌─────────────────────────────────────────────┐
│              Presentation Layer             │
│  (Controllers, DTOs, Middleware)            │
├─────────────────────────────────────────────┤
│              Application Layer              │
│  (Use Cases, Services)                      │
├─────────────────────────────────────────────┤
│               Domain Layer                  │
│  (Entities, Value Objects, Domain Services) │
├─────────────────────────────────────────────┤
│            Infrastructure Layer             │
│  (Repositories, External Services)          │
└─────────────────────────────────────────────┘
```

## コンポーネント設計

### Domain Layer

```typescript
// Entity
class User {
  readonly id: UserId;
  readonly email: Email;
  name: UserName;
  status: UserStatus;
}

// Value Objects
class UserId { value: string; }
class Email { value: string; }
class UserName { value: string; }
```

### Application Layer

```typescript
// Use Cases
class CreateUserUseCase {
  execute(input: CreateUserInput): Promise<User>;
}

class GetUserUseCase {
  execute(userId: UserId): Promise<User>;
}

class UpdateUserUseCase {
  execute(userId: UserId, input: UpdateUserInput): Promise<User>;
}

class DeleteUserUseCase {
  execute(userId: UserId): Promise<void>;
}
```

### Infrastructure Layer

```typescript
// Repository Interface (Domain)
interface IUserRepository {
  findById(id: UserId): Promise<User | null>;
  findByEmail(email: Email): Promise<User | null>;
  save(user: User): Promise<void>;
  delete(id: UserId): Promise<void>;
}

// Repository Implementation (Infrastructure)
class PostgresUserRepository implements IUserRepository {
  // 実装
}
```

## シーケンス図

### ユーザー登録フロー

```
Client -> Controller -> CreateUserUseCase -> UserRepository -> Database
   |          |               |                    |             |
   |  POST /users             |                    |             |
   |          |-------------->|                    |             |
   |          |               |--findByEmail------>|             |
   |          |               |                    |--SELECT---->|
   |          |               |<-------------------|<------------|
   |          |               |                    |             |
   |          |               |--save------------->|             |
   |          |               |                    |--INSERT---->|
   |          |<--------------|<-------------------|<------------|
   |<---------|               |                    |             |
```

## 依存関係

- @design-auth-service - 認証サービス（本サービスを呼び出す）
- #design-auth-calls-user - 認証→ユーザーの依存

## 関連ドキュメント

- @spec-user-api - API仕様
- @internal-user-repository - 内部仕様（リポジトリ実装）
