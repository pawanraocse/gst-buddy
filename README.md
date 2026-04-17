# 🏭 GstBuddy

GSTBuddy is an AI-driven GST document intelligence system designed to interpret GST queries using extensive rule coverage instead of fixed rule engines.

## What You Get

| Feature | Status | Description |
|---------|--------|-------------|
| **Authentication** | ✅ | Cognito-powered signup, login, Google OAuth |
| **API Gateway** | ✅ | Security, routing |
| **Simple Data Model** | ✅ | Single database, shared tables |

---

## 🚀 Deployment Environments

| Environment | Infrastructure | Cost | Use Case |
|-------------|---------------|------|----------|
| **Local** | Docker Compose | $0 | Development |
| **Budget** | EC2 + RDS Free Tier | ~$15-30/mo | Testing/Demo |
| **Production** | ECS Fargate + ALB | ~$150/mo | Production |

---

## 📋 Prerequisites

### Required Tools
- **AWS CLI** - `brew install awscli` then `aws configure`
- **Terraform** >= 1.0.0 - `brew install terraform`
- **Docker** and Docker Compose
- **Node.js** >= 18 (for frontend)
- **Java 21** (for local builds)

### Required Values (Before AWS Deployment)

<details>
<summary><b>1. GitHub Access Token</b> (Required for all AWS deployments)</summary>

1. Go to: **GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)**
2. Click **Generate new token (classic)**
3. Select scope: `repo` (Full control of private repositories)
4. Copy the token: `ghp_xxxxxxxxxxxx`

**Usage** (choose one):
```bash
# Option A: Environment variable (recommended - no secrets in files)
export TF_VAR_github_access_token="ghp_xxx"

# Option B: In terraform.tfvars (gitignored)
github_access_token = "ghp_xxx"
```
</details>

<details>
<summary><b>2. ACM Certificate ARN</b> (Production only - for HTTPS)</summary>

```bash
# Request certificate via CLI
aws acm request-certificate \
  --domain-name "api.yourdomain.com" \
  --validation-method DNS \
  --region ap-south-1

# Output: arn:aws:acm:ap-south-1:123456789012:certificate/xxx
```

**Or via AWS Console:**
1. Go to **AWS Certificate Manager** (in ap-south-1 region)
2. Click **Request certificate** → **Request public certificate**
3. Enter domain name (e.g., `api.yourdomain.com`)
4. Choose **DNS validation**
5. If using Route53, click **Create records in Route53**
6. Wait for status to become **Issued**
7. Copy the **ARN**

> **Note**: Budget deployment doesn't need this (uses HTTP)
</details>

<details>
<summary><b>3. Your Public IP</b> (For bastion SSH access)</summary>

```bash
# Get your current public IP
curl ifconfig.me
# Example output: 203.45.67.89

# Use in CIDR format (add /32 for single IP)
bastion_allowed_ssh_cidrs = ["203.45.67.89/32"]
```
</details>

<details>
<summary><b>4. SSH Key Pair</b> (For EC2/Bastion access)</summary>

```bash
# Generate new key pair (if you don't have one)
ssh-keygen -t rsa -b 4096 -f ~/.ssh/aws-deploy -N ""

# Get the public key for Terraform
cat ~/.ssh/aws-deploy.pub
# ssh-rsa AAAA... user@hostname

# Use in terraform.tfvars
bastion_ssh_public_key = "ssh-rsa AAAA..."

# Use private key for SSH access
SSH_KEY=~/.ssh/aws-deploy ./scripts/budget/deploy.sh
```
</details>

### Quick Reference Table

| Variable | How to Get | Required For |
|----------|------------|--------------|
| `github_access_token` | GitHub Settings → Developer → Tokens | Both environments |
| `frontend_repository_url` | Your GitHub repo URL | Both environments |
| `acm_certificate_arn` | AWS ACM Console or CLI | Production only |
| `bastion_allowed_ssh_cidrs` | `curl ifconfig.me` → add `/32` | SSH to bastion |
| `bastion_ssh_public_key` | `cat ~/.ssh/id_rsa.pub` | SSH to bastion |

---

## 🖥️ Local Development

```bash
# 1. Start all services
docker-compose up -d

# 2. Wait for services to be healthy
docker-compose ps

# 3. Create system admin
./scripts/bootstrap-system-admin.sh admin@example.com "Password123!"

# 4. Access the app
open http://localhost:4200
```

### Local Services
| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Gateway | http://localhost:8080 |
| Auth | http://localhost:8081 |
| Backend | http://localhost:8082 |


---

## 🔄 Agent State Sync

Synchronize agent memory, skills, and environment files across machines without committing to Git.

```bash
# 1. Backup local state to S3
./scripts/local/agent-state.sh push

# 2. On another machine: pull state
./scripts/local/agent-state.sh pull

# 3. Optional: Cleanup S3 after transfer
./scripts/local/agent-state.sh cleanup
```

**Included Patterns**: `.memory/`, `.agents/`, `.agent-skills`, `.env*`, `terraform/cognito-config.env`.

---

## 💰 Budget Deployment (EC2 + RDS)

Runs Docker Compose on a single EC2 instance with managed RDS and ElastiCache.

### One-Shot Deployment ⚡

```bash
# Configure
cd terraform/envs/budget
cp terraform.tfvars.example terraform.tfvars
# Edit: frontend_repository_url, github_access_token, ssh_public_key

# Note: project_name is set in terraform/common.auto.tfvars

# Deploy everything (infra + app + start)
SSH_KEY=~/.ssh/your-key.pem ./scripts/budget/deploy.sh
```

**That's it!** Infrastructure + application deployed in one command.

### Manual Steps (if needed)

<details>
<summary>Click for step-by-step deployment</summary>

#### Step 1: Configure Variables
```bash
cd terraform/envs/budget
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values
```

#### Step 2: Deploy Infrastructure Only
```bash
./scripts/budget/deploy.sh
```

#### Step 3: Deploy Application Manually
```bash
scp -i <key.pem> -r . ec2-user@<EC2_IP>:/app/
ssh -i <key.pem> ec2-user@<EC2_IP>
cd /app && ./scripts/budget/start.sh
```
</details>

### Access
- **Frontend**: Amplify URL (shown in output)
- **API**: http://<EC2_IP>:8080
- **Cost**: ~$15-30/month

---

## 🏭 Production Deployment (ECS Fargate)

Full AWS deployment with auto-scaling, load balancing, and HTTPS.

### One-Shot Deployment ⚡

```bash
# Configure
cd terraform/envs/production
cp terraform.tfvars.example terraform.tfvars
# Edit: acm_certificate_arn, frontend_repository_url, github_access_token

# Deploy everything (infra + build + push + deploy)
./scripts/production/deploy.sh
```

**That's it!** Infrastructure + Docker images + ECS services deployed.

### CI/CD (Automatic Deploys)

After initial setup, push to `main` triggers automatic deployment:

```bash
# Setup GitHub Secrets:
# - AWS_ACCESS_KEY_ID
# - AWS_SECRET_ACCESS_KEY

# Then just push code
git push origin main
# → GitHub Actions builds, pushes, and deploys automatically
```

### Manual Steps (if needed)

<details>
<summary>Click for step-by-step deployment</summary>

#### Step 1: Configure Variables
```bash
cd terraform/envs/production
cp terraform.tfvars.example terraform.tfvars
# REQUIRED: acm_certificate_arn, frontend_repository_url, github_access_token
```

#### Step 2: Deploy Infrastructure
```bash
./scripts/production/deploy.sh
```

#### Step 3: Push Docker Images
```bash
./scripts/production/push-ecr.sh
```

#### Step 4: Trigger ECS Deployment
```bash
aws ecs update-service --cluster GSTbuddies-production --service gateway --force-new-deployment
```
</details>

### Access
- **Frontend**: Amplify URL (shown in output)
- **API**: https://<ALB_DNS_NAME>
- **Cost**: ~$150/month

---

## 🔧 Configuration Reference

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `SPRING_DATASOURCE_URL` | JDBC connection string | Yes |
| `SPRING_DATASOURCE_PASSWORD` | DB password (from Secrets Manager) | Yes |
| `REDIS_HOST` | Redis endpoint | Yes |

| `AWS_COGNITO_USER_POOL_ID` | Cognito pool ID | Yes |

### SSM Parameters (Auto-created)

| Parameter | Description |
|-----------|-------------|
| `/<project>/<env>/rds/endpoint` | Database hostname |
| `/<project>/<env>/rds/port` | Database port |
| `/<project>/<env>/rds/database` | Database name |
| `/<project>/<env>/redis/endpoint` | Redis hostname |

---

## 📁 Project Structure

```
├── frontend/              # Angular app
├── gateway-service/       # API gateway
├── auth-service/          # Authentication
├── backend-service/       # Your domain logic
├── common-infra/          # Shared infrastructure
├── terraform/
│   ├── modules/           # Reusable Terraform modules
│   │   ├── vpc/
│   │   ├── rds/
│   │   ├── elasticache/
│   │   ├── ecr/
│   │   ├── ecs-cluster/
│   │   ├── ecs-service/
│   │   ├── alb/
│   │   ├── amplify/
│   │   └── bastion/
├── docker-compose.yml         # Local development
├── docker-compose.base.yml    # Common service definitions
├── docker-compose.budget.yml  # Budget env (external DB)
├── project.config             # Central project configuration
└── terraform/
    ├── common.auto.tfvars     # Shared config (project_name, aws_region)
    ├── modules/               # Reusable Terraform modules
    └── envs/
        ├── budget/            # Budget deployment config
        └── production/        # Production deployment config
```

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [HLD.md](HLD.md) | Architecture, design decisions |
| [docs/QUICK_START.md](docs/QUICK_START.md) | Getting started guide |
| [docs/CONFIGURATION.md](docs/CONFIGURATION.md) | **Central config reference** |
| [docs/DEBUGGING.md](docs/DEBUGGING.md) | **Troubleshooting & logs** |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Feature status, future plans |
| [docs/AWS_DEPLOYMENT.md](docs/AWS_DEPLOYMENT.md) | AWS deployment guide |

---

## 🛠️ Adding Your Service

1. **Copy the backend-service template**
   ```bash
   cp -r backend-service/ my-service/
   ```

2. **Update configuration**
   ```yaml
   # application.yml
   spring.application.name: my-service
   server.port: 8084
   ```

3. **Add your domain logic**
   - Add your domain logic

4. **Register in docker-compose.yml**

---

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.5.9, Spring Cloud Gateway
- **Frontend:** Angular 21, PrimeNG
- **Database:** PostgreSQL
- **Cache:** Redis (ElastiCache)
- **Auth:** AWS Cognito + Lambda
- **Infrastructure:** Terraform, Docker, ECS Fargate

---

**License:** MIT  
**Version:** 1.0.0
