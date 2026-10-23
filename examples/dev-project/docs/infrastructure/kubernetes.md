---
id: infra-k8s-user
title: ユーザーサービスKubernetes設定
domain: infrastructure
type: Deployment
namespace: production
---

# Kubernetes デプロイ設定

## 概要

本ドキュメントは、ユーザーサービスおよび認証サービスのKubernetesデプロイ設定を定義する。

## 対応する内部仕様

- @internal-user-repository - ユーザーリポジトリ実装をデプロイ
- @internal-jwt-handler - JWTハンドラ実装をデプロイ
- #deploy-functor - 内部仕様→インフラの対応関係

## ユーザーサービス Deployment

```yaml
# user-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: production
  labels:
    app: user-service
    tier: backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: registry.example.com/user-service:v1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: url
        - name: LOG_LEVEL
          value: "info"
        resources:
          requests:
            cpu: "100m"
            memory: "256Mi"
          limits:
            cpu: "500m"
            memory: "512Mi"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

## 認証サービス Deployment

```yaml
# auth-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: production
  labels:
    app: auth-service
    tier: backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: registry.example.com/auth-service:v1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: url
        - name: REDIS_URL
          valueFrom:
            secretKeyRef:
              name: redis-credentials
              key: url
        - name: JWT_PRIVATE_KEY
          valueFrom:
            secretKeyRef:
              name: jwt-keys
              key: private
        - name: JWT_PUBLIC_KEY
          valueFrom:
            secretKeyRef:
              name: jwt-keys
              key: public
        - name: USER_SERVICE_URL
          value: "http://user-service:8080"
        resources:
          requests:
            cpu: "100m"
            memory: "256Mi"
          limits:
            cpu: "500m"
            memory: "512Mi"
```

## Service 設定

```yaml
# services.yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: production
spec:
  selector:
    app: user-service
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: production
spec:
  selector:
    app: auth-service
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
```

## HPA（水平Pod自動スケーリング）

```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

## 依存関係

- @infra-database - データベースに接続
- @infra-k8s-auth - 認証サービス
- #infra-user-uses-db, #infra-auth-uses-db - DB接続の依存

## 関連ドキュメント

- @internal-user-repository - 内部仕様
- @internal-jwt-handler - JWT実装
- @infra-database - データベース設定
