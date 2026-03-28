# Project Scripts Documentation

All utility scripts for building, deploying, and managing GSTbuddies across environments.

---

## 📁 Directory Structure

```
scripts/
├── README.md                      # This file
├── bootstrap-system-admin.sh      # Create a super-admin user (any env)
├── budget/
│   ├── deploy.sh                  # Full budget deployment (Terraform + EC2)
│   ├── destroy.sh                 # Tear down budget infrastructure
│   ├── manage.sh                  # Manage budget services (restart, logs)
│   ├── start.sh                   # Start services on EC2 (Docker Compose)
│   └── db-tunnel.sh               # SSH tunnel to budget RDS (pgAdmin)
├── production/
│   ├── deploy.sh                  # Full production deployment (ECS)
│   ├── destroy.sh                 # Tear down production infrastructure
│   ├── push-ecr.sh                # Push Docker images to ECR
│   └── db-tunnel.sh               # SSH tunnel to production RDS (pgAdmin)
├── local/
│   ├── deploy.sh                  # Deploy Cognito for local dev
│   ├── destroy.sh                 # Destroy local Cognito
│   ├── import-ssm.sh              # Import SSM parameters
│   └── delete-ssm.sh              # Delete SSM parameters
├── env/
│   └── export-envs.sh             # Export env vars from .env files
├── testing/
│   ├── test-api-key.sh            # Test API key auth
│   └── test-spawn-project.sh      # Test project creation
└── ai-toolkit/
    ├── cli.sh                     # AI toolkit CLI
    ├── filter-logs.sh             # Filter logs
    └── query-db.sh                # Query DB helper
```

---

## 🚀 Deployment Workflows

### Budget Environment (EC2 + Docker Compose, ~$30-40/month)

```bash
# 1. Deploy infrastructure + services
./scripts/budget/deploy.sh

# 2. Bootstrap super-admin user
./scripts/bootstrap-system-admin.sh "your-email@example.com" "YourPassword123!"

# 3. (Optional) Connect to DB
sshgstbudget   # alias, or: ./scripts/budget/db-tunnel.sh
```

### Production Environment (ECS + Fargate, ~$150/month)

```bash
# 1. Deploy infrastructure + build + push to ECS
./scripts/production/deploy.sh

# 2. Bootstrap super-admin user
./scripts/bootstrap-system-admin.sh "your-email@example.com" "YourPassword123!" production

# 3. (Optional) Connect to DB
sshgstprod   # alias, or: ./scripts/production/db-tunnel.sh
```

### Local Development

```bash
# 1. Deploy Cognito to AWS
./scripts/local/deploy.sh

# 2. Load environment variables
source ./scripts/env/export-envs.sh

# 3. Start services
docker-compose up -d
```

---

## 👤 Bootstrap System Admin

Creates a super-admin user in Cognito and links it to the database.

**Usage:**
```bash
./scripts/bootstrap-system-admin.sh <email> <password> [environment]
```

| Arg | Description | Default |
|---|---|---|
| `email` | Admin email address | interactive prompt |
| `password` | Admin password (upper+lower+number+symbol) | interactive prompt |
| `environment` | `budget`, `production`, or `local` | `budget` |

**What it does:**
1. Creates (or updates) user in Cognito
2. Sets permanent password
3. Assigns `super-admin` role + `admin` group in Cognito
4. Links to database via API (primary) or direct DB insert (fallback)
5. Cleans up `SYSTEM_ADMIN_PLACEHOLDER` seeded record

**Fallback mechanism:** If the API call fails (service down, auth issue, etc.), the script automatically SSHs into EC2 and inserts the user, role, and credit wallet directly into RDS.

---

## 🔗 DB Tunnel Scripts

Open an SSH tunnel to RDS for use with pgAdmin, DBeaver, TablePlus, or any DB client.

### Budget

```bash
./scripts/budget/db-tunnel.sh [port]    # default: 5433
# or use the alias:
sshgstbudget
```

### Production

```bash
./scripts/production/db-tunnel.sh [port]  # default: 5434
# or use the alias:
sshgstprod
```

**Connect your DB client to:**

| Setting | Budget | Production |
|---|---|---|
| Host | `localhost` | `localhost` |
| Port | `5433` | `5434` |
| Database | auto-displayed | auto-displayed |
| Username | auto-displayed | auto-displayed |
| Password | auto-displayed | auto-displayed |

All connection details are fetched from SSM and printed when the tunnel opens.

**Shell aliases** (added to `~/.zshrc`):
```bash
alias sshgstbudget='~/prototype/GSTbuddies/scripts/budget/db-tunnel.sh'
alias sshgstprod='~/prototype/GSTbuddies/scripts/production/db-tunnel.sh'
```

---

## 🔧 Environment Scripts

### `env/export-envs.sh`

Exports environment variables from `.env` files to the current shell.

```bash
source ./scripts/env/export-envs.sh
```

---

## 🔐 Security Notes

1. **AWS Profiles:** Budget uses `personal`, production uses `production`
2. **Never commit:** `.env`, `cognito-config.env`, SSM parameter values
3. **SSM Parameters:** Secrets stored as `SecureString` type
4. **DB passwords:** Stored in AWS Secrets Manager, fetched dynamically by scripts

---

## 🐛 Troubleshooting

### Bootstrap admin can't access admin panel (500 error)
The user exists in Cognito but has no `super-admin` role in the database. Re-run the bootstrap script — it will use the DB fallback to fix it:
```bash
./scripts/bootstrap-system-admin.sh "admin@example.com" "password"
```

### DB tunnel won't connect
- Ensure EC2 instance is running: `aws ec2 describe-instances`
- Check SSH key exists: `ls ~/.ssh/id_rsa_personal`
- Verify security group allows SSH (port 22)

### AWS credentials error
```bash
aws configure --profile personal
# or
export AWS_PROFILE=personal
```

---

**Last Updated:** 2026-03-07
**Maintained By:** DevOps Team
