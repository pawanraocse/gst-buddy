---
description: Run diagnostic checks for the prod_init environment, including EC2 status, logs, and Amplify build progress.
---

# Debug & Root Cause Analysis Skill

This skill provides automated tools and recipes to pinpoint issues in the `prod_init` environment (EC2 + RDS).

### ⚙️ Production Profile (prod_init)
```bash
# ENV_IDENTITY="prod_init"
# EC2_IP="65.1.250.35"
# DB_HOST="gstbuddies-prod-init.czs4w640o6ge.ap-south-1.rds.amazonaws.com"
# DB_NAME="gstbuddies_db"
# DB_USER="postgres"
```

## 🛠️ Debug Toolkit

Use these scripts in `scripts/prod_init/` for instant diagnostics:

1. **`./scripts/prod_init/health-check.sh`**
   - Quick check of EC2 status, Docker containers, and RDS connectivity.
   - Use this FIRST for any "site is down" report.

2. **`./scripts/prod_init/db-query.sh "SQL"`**
   - Run SQL queries directly against production.
   - Handles all AWS secrets and SSH tunneling automatically.
   - Example: `./scripts/prod_init/db-query.sh "SELECT * FROM user_credit_wallets WHERE consumed_credits > 10;"`

3. **`./scripts/prod_init/logs-tail.sh <service> [lines] [grep]`**
   - Real-time log access.
   - Example: `./scripts/prod_init/logs-tail.sh auth-service 500 "ERROR"`

## 📋 Root Cause Recipes

### 1. Credit Discrepancy ("Why are my credits gone?")
- Run: `./scripts/prod_init/db-query.sh "SELECT * FROM credit_transactions WHERE user_id = 'USER_ID' ORDER BY created_at DESC LIMIT 20;"`
- Check for bursts of small transactions (indicates a loop or parallel document processing).

### 2. Internal Server Error (500)
- Run: `./scripts/prod_init/logs-tail.sh <service> 200 "Exception"`
- Look for `ObjectOptimisticLockingFailureException` (concurrency issue) or `NullPointerException`.

### 3. Payment Failure
- Run: `./scripts/prod_init/logs-tail.sh auth-service 500 "Razorpay"`
- Verify if `RAZORPAY_KEY_ID` or `SECRET` is missing in logs during startup.

### 4. Amplify Build Issues
- Command: `aws amplify list-jobs --app-id d3mnggqk8iw31y --branch-name prod --max-items 1 --region ap-south-1`

### 5. Verify SSM Secret (Razorpay)
- Command: `aws ssm get-parameter --name "/gstbuddies/prod_init/razorpay/key_id" --with-decryption --query "Parameter.Value" --output text --region ap-south-1`
