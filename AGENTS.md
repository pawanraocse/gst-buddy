# AGENTS.md — Project Memory for AI Coding Agents

> **Read this first.** This file gives any AI coding agent full context to work on GST Buddy without rediscovering the codebase. It saves tokens and ensures architectural consistency.

---

## 🎯 What Is This Project?

**GST Buddy** — Multi-tenant SaaS platform for Indian GST compliance. Businesses upload supplier payment ledgers (Excel) and get automated Rule 37 ITC reversal calculations, interest liability, and at-risk supplier identification.

**Business model:** Credit-based (pay-per-use). Trial = 2 free credits, plans via Razorpay (INR).

---

## 🏗️ Architecture (Read This Carefully)

```
Angular SPA (Amplify Auth) → CloudFront → Gateway (8080) → Eureka Discovery
                                              ├── Auth Service (8081) ← Cognito, PostgreSQL, Razorpay
                                              └── Backend Service (8082) ← Excel parsing, Rule 37 engine
```

**Key principle:** Gateway validates JWTs and injects `X-Tenant-Id`, `X-User-Id`, `X-Email` headers. Internal services **trust these headers** — they do NOT re-validate JWTs.

### Modules

| Module | Port | What it does |
|--------|------|---|
| `gateway-service/` | 8080 | API Gateway — JWT validation, Eureka routing, rate limiting (Redis), circuit breakers, header enrichment |
| `auth-service/` | 8081 | Identity — Cognito integration, signup pipeline, RBAC (`@RequirePermission`), credit wallet, Razorpay payments, invitations, referrals |
| `backend-service/` | 8082 | Business logic — Excel upload, Rule 37 calculation, export. Consumes credits via `CreditClient` → auth-service |
| `eureka-server/` | 8761 | Service discovery |
| `common-dto/` | — | Shared DTOs, enums, constants (`HeaderNames`, `ErrorCodes`), `TenantContext` |
| `common-infra/` | — | Cross-cutting — `TenantFilter`, multi-tenant routing, caching (Caffeine+Redisson), `@RequirePermission` annotation |
| `frontend/` | 4200 | Angular 21 SPA — PrimeNG UI, AWS Amplify auth, admin panel |
| `terraform/` | — | Full AWS IaC (12 modules) |

### Gateway Routing (IMPORTANT)

```
/auth/**         → Auth Service (preserveHostHeader, context path = /auth)
/auth-service/** → Auth Service (rewritePath to /auth/*)
/backend-service/** → Backend Service (stripPrefix)
```

Admin endpoints are in auth-service at `/api/v1/admin/**`. Through gateway they're `/auth/api/v1/admin/**`.

The `SecurityConfig` in gateway determines which paths skip JWT. The `SecurityConfiguration` in auth-service determines which paths skip Spring Security. Both must be updated when adding new public/internal endpoints.

### Auth Flow

1. **Signup:** Angular → Gateway → Auth Service → Cognito SDK (creates user) → PostConfirmation Lambda (marks verified)
2. **Login:** Angular → Amplify SDK → Cognito (direct) → JWT with `custom:tenantId` claim
3. **API calls:** Angular sends JWT → Gateway validates → injects headers → routes to service
4. **Admin bootstrap:** Script creates Cognito user → API/DB fallback links to database with `super-admin` role

### Multi-Tenancy

- Shared DB, `tenant_id` discriminator column on every table
- `TenantFilter` (common-infra) reads `X-Tenant-Id` header and sets `TenantContext`
- JWTs contain `custom:tenantId` claim (injected by PreTokenGeneration Lambda)

---

## 🗂️ Key Files to Know

| File | Why it matters |
|---|---|
| `PROJECT_INDEX.md` | Full architectural map — API endpoints, DB schema, env vars, integrations |
| `HLD.md` | High-level design with architecture diagram, links to all docs |
| `project.config` | Central config (project name, ports, DB settings, AWS naming) |
| `.env` | Local env vars (AWS creds, Stripe/Razorpay keys, ports) |
| `docker-compose.yml` | Local dev (all services) |
| `docker-compose.budget.yml` | Budget deployment (EC2 Docker Compose) |
| `terraform/envs/budget/main.tf` | Budget infrastructure (EC2, RDS, CloudFront, Cognito, Amplify) |
| `terraform/envs/production/` | Production infrastructure (ECS, ALB, RDS, ElastiCache) |
| `scripts/README.md` | All scripts documented |

---

## 🚀 Environments

| Environment | How it runs | Deploy command | API URL |
|---|---|---|---|
| **Local** | Docker Compose | `docker-compose up -d` | `http://localhost:8080` |
| **Budget** | EC2 + Docker Compose | `./scripts/budget/deploy.sh` | CloudFront URL (SSM: `/gst-buddy/budget/api/url`) |
| **Production** | ECS Fargate | `./scripts/production/deploy.sh` | ALB URL |

### Budget Deployment Flow
```
deploy.sh → Terraform (infra) → Build JARs → rsync to EC2 → Docker build on EC2 → start.sh (sequential service startup)
```

### Admin Bootstrap (post-deploy)
```bash
./scripts/bootstrap-system-admin.sh "email" "password" [environment]
# Handles: Cognito user + DB linking (API primary, SSH+DB fallback)
```

### DB Access
```bash
sshgstbudget    # SSH tunnel to budget RDS on localhost:5433
sshgstprod      # SSH tunnel to prod RDS on localhost:5434
```

---

## 🧩 Code Conventions

### Java (Backend)
- **Java 21**, Spring Boot 3.5.9, Spring Cloud 2025.0.0
- Multi-module Maven project, `common-infra` and `common-dto` are shared libraries
- `@RequirePermission(resource="user", action="manage")` — AOP-based authorization via `AuthorizationAspect`
- Signup uses action pipeline pattern: `CreateCognitoUserAction → GrantTrialCreditsAction → SendVerificationEmailAction`
- DB migrations: Flyway (`flyway_auth_history`, `flyway_schema_history_backend`)
- Credit system: Immutable transaction ledger + optimistic locking on `user_credit_wallets`
- Context path: auth-service = `/auth`, backend-service = none

### Angular (Frontend)
- Angular 21, PrimeNG 21, PrimeFlex 4
- AWS Amplify SDK for Cognito auth
- Feature modules: `features/auth/`, `features/dashboard/`, `features/admin/`, `features/rule37/`
- Environment config generated at build time via `scripts/generate-env.js`
- Guard: `adminGuard` checks `custom:role === 'super-admin'` in JWT

### Terraform
- 12 reusable modules in `terraform/modules/`
- Environment-specific configs in `terraform/envs/{budget,production}/`
- Shared vars in `terraform/common.auto.tfvars`
- SSM Parameter Store for all config, Secrets Manager for DB passwords

---

## 📊 Database Schema

### Auth Service (Flyway-managed)
`users` → `user_roles` → `roles` → `role_permissions` → `permissions`
`user_credit_wallets` → `credit_transactions`
`invitations`, `acl_entries`, `group_role_mappings`, `plans`, `referrals`

### Backend Service (Flyway-managed)
`rule37_calculation_runs` (JSONB for results, auto-expiry via `RetentionScheduler`)

### Seeded Roles
`super-admin` (18 permissions), `admin`, `editor`, `viewer`, `guest`

---

## 🔧 Current State (Updated: 2026-03-07)

### ✅ Completed
- Full microservices architecture (gateway, auth, backend, eureka)
- Multi-tenant RBAC with AOP-based permission checks
- Cognito integration (signup, login, OAuth2, MFA-ready)
- Admin panel (dashboard, user mgmt, plan mgmt, credit overview)
- Rule 37 calculation engine with Excel upload/export
- Credit/billing system (Razorpay integration)
- Referral system with credit rewards
- Budget deployment (EC2 + Docker Compose + CloudFront)
- Production deployment scaffolding (ECS + Fargate)
- Bootstrap script with DB fallback
- DB tunnel scripts + shell aliases
- Comprehensive test suite (JUnit 5, Mockito, Testcontainers)

### 🔄 Recent Changes (2026-03-07)
- Fixed `SecurityConfiguration` to permit `/api/v1/admin/bootstrap/**` (was missing wildcard)
- Fixed gateway `SecurityConfig` to permit bootstrap endpoint without JWT
- Enhanced `bootstrap-system-admin.sh` with direct DB fallback via SSH
- Added `db-tunnel.sh` scripts for budget + production
- Added `sshgstbudget`/`sshgstprod` aliases to `~/.zshrc`

### ⚠️ Known Issues / Tech Debt
- Frontend env configs have hardcoded Cognito IDs (should use build-time injection)
- System tests skipped by default, no CI gate enforcing them
- No user-configurable data retention (7-day default hardcoded)
- Production deployment not yet tested end-to-end (ECS scaffold exists)
- `.github/copilot-instructions.md` is empty

### 📋 Planned
- Stripe integration (US/international payments)
- SSO support (SAML/OIDC for enterprise tenants)
- Bulk operations in admin panel
- Audit logging
- Custom domain support

---

## ⚡ Quick Reference for Common Tasks

### Add a new API endpoint
1. Add controller method in auth-service or backend-service
2. Add `@RequirePermission` if admin-only
3. If public/internal: add to both `SecurityConfiguration.java` (auth-service) AND `SecurityConfig.java` (gateway)
4. If admin: already covered by `/api/v1/admin/**` permitAll + `@RequirePermission` AOP

### Add a new DB table
1. Create Flyway migration in `{service}/src/main/resources/db/migration/`
2. Naming: `V{N}__{description}.sql`
3. Add `tenant_id` column for multi-tenant tables

### Deploy code changes to budget
1. `./scripts/budget/deploy.sh` (full) or `./scripts/budget/deploy.sh --rebuild` (force JAR rebuild)
2. Services auto-rebuild Docker images on EC2

### Debug a budget environment issue
1. SSH: `ssh -i ~/.ssh/id_rsa_personal ec2-user@<EC2_IP>`
2. Logs: `docker logs gst-buddy-auth-service --tail 100`
3. DB: `sshgstbudget` then connect pgAdmin to `localhost:5433`
