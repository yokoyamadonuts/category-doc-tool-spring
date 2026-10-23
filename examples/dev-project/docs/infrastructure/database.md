---
id: infra-database
title: PostgreSQLデータベース
domain: infrastructure
type: StatefulSet
namespace: production
---

# PostgreSQL データベース設定

## 概要

本ドキュメントは、PostgreSQLデータベースのKubernetesデプロイ設定を定義する。

## 依存関係

このデータベースは以下のサービスから接続される：
- @infra-k8s-user - ユーザーサービス
- @infra-k8s-auth - 認証サービス（@id:infra-k8s-auth として category.yaml に定義）
- #infra-user-uses-db, #infra-auth-uses-db - 接続の依存関係

## StatefulSet 設定

```yaml
# postgres-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: production
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: "app_production"
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password
        - name: PGDATA
          value: /var/lib/postgresql/data/pgdata
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            cpu: "500m"
            memory: "1Gi"
          limits:
            cpu: "2000m"
            memory: "4Gi"
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "ssd"
      resources:
        requests:
          storage: 100Gi
```

## Service 設定

```yaml
# postgres-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: production
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP
```

## Secret 設定

```yaml
# database-secrets.yaml (暗号化して保存)
apiVersion: v1
kind: Secret
metadata:
  name: database-credentials
  namespace: production
type: Opaque
data:
  username: <base64-encoded>
  password: <base64-encoded>
  url: <base64-encoded>  # postgres://user:pass@postgres:5432/app_production
```

## バックアップ設定

```yaml
# backup-cronjob.yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
  namespace: production
spec:
  schedule: "0 2 * * *"  # 毎日2:00
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: postgres:15
            command:
            - /bin/sh
            - -c
            - |
              pg_dump -h postgres -U $PGUSER $PGDATABASE | \
              gzip > /backup/backup-$(date +%Y%m%d).sql.gz
            env:
            - name: PGUSER
              valueFrom:
                secretKeyRef:
                  name: database-credentials
                  key: username
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: database-credentials
                  key: password
            - name: PGDATABASE
              value: "app_production"
            volumeMounts:
            - name: backup-storage
              mountPath: /backup
          restartPolicy: OnFailure
          volumes:
          - name: backup-storage
            persistentVolumeClaim:
              claimName: backup-pvc
```

## モニタリング

```yaml
# postgres-exporter.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-exporter
  namespace: production
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres-exporter
  template:
    metadata:
      labels:
        app: postgres-exporter
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "9187"
    spec:
      containers:
      - name: exporter
        image: prometheuscommunity/postgres-exporter
        ports:
        - containerPort: 9187
        env:
        - name: DATA_SOURCE_NAME
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: url
```

## 接続情報

| 項目 | 値 |
|------|-----|
| ホスト | postgres.production.svc.cluster.local |
| ポート | 5432 |
| データベース | app_production |
| 最大接続数 | 100 |

## 関連ドキュメント

- @infra-k8s-user - ユーザーサービス（接続元）
- @internal-user-repository - UserRepository（接続するモジュール）
