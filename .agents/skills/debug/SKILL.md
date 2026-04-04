---
description: Run diagnostic checks for the prod_init environment, including EC2 status, logs, and Amplify build progress.
---

# Debug & Root Cause Analysis Skill

This skill provides two diagnostic toolkits:
1. **`scripts/prod_init/`** — Remote diagnostics on the live EC2/RDS (`prod_init`) environment.
2. **`scripts/ai-toolkit/`** — Local diagnostics for the developer's Docker Compose environment.

Use the correct toolkit based on where the issue is occurring.

---

## ⚙️ Production Profile (prod_init)

> This block is parsed by `scripts/prod_init/env-config.sh`. Keep formatting exact.

```bash
# ENV_IDENTITY="prod_init"
# EC2_IP="65.1.250.35"
# DB_HOST="gstbuddies-prod-init.czs4w640o6ge.ap-south-1.rds.amazonaws.com"
# DB_NAME="gstbuddies_db"
# DB_USER="postgres"
```

> **Note:** `env-config.sh` prefers live SSM values over the above fallbacks. Update
> these only when SSM is unavailable (e.g., total infrastructure loss).

---

## 🛠️ Toolkit 1: Production (Remote EC2 — `prod_init`)

Run from the **project root**. All scripts auto-resolve EC2 IP from SSM, falling back to the profile above.

### 1. `./scripts/prod_init/health-check.sh`
Quick check of EC2 status, Docker container health, and RDS TCP connectivity.
**Use this FIRST for any "site is down" report.**

```bash
./scripts/prod_init/health-check.sh
```

### 2. `./scripts/prod_init/db-query.sh [SQL]`
Run SQL queries directly against production RDS via SSH tunnel. Fetches DB password from Secrets Manager automatically.

```bash
# Specific query
./scripts/prod_init/db-query.sh "SELECT * FROM user_credit_wallets WHERE consumed_credits > 10;"

# Interactive psql session
./scripts/prod_init/db-query.sh
```

### 3. `./scripts/prod_init/logs-tail.sh <service> [lines] [grep]`
Tail live Docker logs from the EC2 instance. Supports grep filtering.

```bash
./scripts/prod_init/logs-tail.sh auth-service 500 "ERROR"
./scripts/prod_init/logs-tail.sh gateway-service 200
```

**Valid service names:** `auth-service`, `backend-service`, `gateway-service`, `eureka-server`

---

## 🛠️ Toolkit 2: Local Development (Docker Compose — `ai-toolkit`)

Run from the **project root**. Operates against the local Docker Compose stack.

### 1. `./scripts/ai-toolkit/cli.sh query-db "<SQL>"`
Query the local PostgreSQL database (returns JSON). Use this to inspect local state without bloating the AI context window.

```bash
./scripts/ai-toolkit/cli.sh query-db "SELECT id, email FROM users LIMIT 10;"
```

### 2. `./scripts/ai-toolkit/cli.sh filter-logs <log-file>`
Extract only `ERROR`/`WARN` lines and stack traces from a local Spring Boot log file.

```bash
./scripts/ai-toolkit/cli.sh filter-logs backend-service/target/spring.log
```

### 3. `./scripts/ai-toolkit/get-skeleton.sh <file-or-dir>`
Extract class/method signatures from Java, TypeScript, or Python files. Use this before reading large source files to avoid consuming unnecessary tokens.

```bash
./scripts/ai-toolkit/get-skeleton.sh auth-service/src/main/java/
./scripts/ai-toolkit/get-skeleton.sh auth-service/src/main/java/com/learning/authservice/auth/service/AuthServiceImpl.java
```

---

## 📋 Root Cause Recipes

### 1. Credit Discrepancy ("Why are my credits gone?")
```bash
./scripts/prod_init/db-query.sh "SELECT * FROM credit_transactions WHERE user_id = 'USER_ID' ORDER BY created_at DESC LIMIT 20;"
```
Check for bursts of small transactions (indicates a loop or parallel document processing).

### 2. Internal Server Error (500)
```bash
./scripts/prod_init/logs-tail.sh <service> 200 "Exception"
```
Look for `ObjectOptimisticLockingFailureException` (concurrency) or `NullPointerException`.

### 3. Payment Failure
```bash
./scripts/prod_init/logs-tail.sh auth-service 500 "Razorpay"
```
Verify if `RAZORPAY_KEY_ID` or `SECRET` is missing in startup logs.

### 4. Amplify Build Issues
```bash
aws amplify list-jobs --app-id d3mnggqk8iw31y --branch-name prod --max-items 1 --region ap-south-1
```

### 5. Verify SSM Secret (Razorpay)
```bash
aws ssm get-parameter --name "/gstbuddies/prod_init/razorpay/key_id" --with-decryption --query "Parameter.Value" --output text --region ap-south-1
```

### 6. Investigate local bug (no EC2 access needed)
```bash
# Check local DB state
./scripts/ai-toolkit/cli.sh query-db "SELECT * FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 5;"

# Scan a service for code-level signatures before reading full files
./scripts/ai-toolkit/get-skeleton.sh backend-service/src/
```
