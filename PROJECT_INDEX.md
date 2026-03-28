# Project Index & Architectural Map

## Project Purpose

**GSTbuddies** is a multi-tenant SaaS platform for Indian GST (Goods and Services Tax) compliance. It enables businesses to upload supplier payment ledgers and automatically calculate Rule 37 ITC (Input Tax Credit) reversals, interest liability, and at-risk supplier identification based on the 180-day payment rule.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Backend** | Java, Spring Boot | 21, 3.5.9 |
| **Microservices** | Spring Cloud (Gateway, Eureka) | 2025.0.0 |
| **Frontend** | Angular, TypeScript | 21, 5.9 |
| **UI Components** | PrimeNG, PrimeFlex | 21.0.0-rc.1, 4.0.0 |
| **Auth SDK** | AWS Amplify (frontend) | 6.15.8 |
| **Database** | PostgreSQL | 16 |
| **Cache** | Redis (Caffeine local + Redisson distributed) | 7.x |
| **Identity** | AWS Cognito + Lambda triggers | — |
| **Payments** | Razorpay SDK | 1.4.6 |
| **IaC** | Terraform | 1.9+ |
| **Observability** | OpenTelemetry, Micrometer, AWS X-Ray | 1.43.0 |
| **CI/CD** | GitHub Actions → ECR → ECS | — |
| **Testing** | JUnit 5, Mockito, AssertJ, Testcontainers, REST Assured | — |

## Module / Folder Map

| Module | Port | Responsibility |
|--------|------|----------------|
| `gateway-service/` | 8080 | API Gateway — JWT validation, routing via Eureka, rate limiting (Redis), circuit breakers, header enrichment (`X-Tenant-Id`, `X-User-Id`) |
| `auth-service/` | 8081 | Identity & access — Cognito integration, signup pipeline, RBAC/PBAC, credit wallet, Razorpay payments, invitations, email verification, account management |
| `backend-service/` | 8082 | Business logic — Rule 37 ledger upload (Excel), interest calculation, FIFO payment matching, Excel export, data retention scheduler |
| `eureka-server/` | 8761 | Service discovery — standalone Netflix Eureka with self-preservation |
| `common-dto/` | — | Shared DTOs, enums (`TenantType`, `IdpType`), constants (`HeaderNames`, `ErrorCodes`), `TenantContext` |
| `common-infra/` | — | Cross-cutting infra — multi-tenant routing, `TenantFilter`, caching (Caffeine/Redisson), distributed locking, rate limiting, HTTP client factory, logging filters |
| `frontend/` | 4200 | Angular 21 SPA — PrimeNG UI, AWS Amplify auth, Rule 37 upload/results, super-admin panel (dashboard, user mgmt, plan mgmt, credit overview), settings |
| `system-tests/` | — | E2E integration tests with REST Assured + Testcontainers |
| `terraform/` | — | Full AWS IaC (12 modules): VPC, RDS, ElastiCache, ECS, Cognito, ALB, ECR, Amplify, Bastion, SSM |
| `terraform/lambdas/` | — | Python Lambda source code for Cognito triggers |
| `docs/` | — | Architecture, authentication, billing, DB schema, deployment, config, debugging, design system, quick start |

## Data-Flow Diagram

```
User → Angular SPA (Amplify Auth)
  → ALB / localhost:8080
    → Gateway Service
      ├── Validates JWT (Cognito JWKS)
      ├── Extracts tenant_id from JWT claims
      ├── Injects X-Tenant-Id, X-User-Id, X-Authorities headers
      ├── Rate limiting (Redis) + Circuit breakers (Resilience4j)
      └── Routes via Eureka discovery:
          ├── /auth/** → Auth Service (8081)
          │     ├── Cognito SDK (signup, login, user mgmt)
          │     ├── PostgreSQL (users, roles, permissions, wallets, transactions)
          │     ├── Razorpay (payment orders, verification)
          │     └── SES (email verification)
          └── /api/** → Backend Service (8082)
                ├── Excel parsing (Apache POI)
                ├── Rule 37 calculation engine (FIFO matching, interest calc)
                ├── PostgreSQL (rule37_calculation_runs with JSONB)
                ├── CreditClient → Auth Service (consume credits)
                └── Excel export (Rule37ExcelExportStrategy)
```

## API Endpoints

### Auth Service (`/auth/api/v1/`)

| Path | Method | Purpose |
|------|--------|---------|
| `/auth/me` | GET | Current user info |
| `/auth/tokens` | GET | JWT tokens from OAuth2 session |
| `/auth/login` | POST | Email/password login |
| `/auth/signup` | POST | User signup |
| `/auth/logout` | POST | Logout |
| `/signup` | POST | Unified signup (personal/organization) |
| `/signup/verify` | POST | Email verification code |
| `/users` | GET | List users (optional status filter) |
| `/users/search?q=` | GET | Search users |
| `/users/{id}` | GET | Get user by ID |
| `/users/stats` | GET | User statistics |
| `/invitations` | GET/POST | List/create invitations |
| `/invitations/{token}/accept` | POST | Accept invitation |
| `/credits/wallet` | GET | Credit wallet balance |
| `/credits/transactions` | GET | Transaction history |
| `/credits/consume` | POST | Deduct credits (internal) |
| `/plans` | GET | Available pricing plans |
| `/payments/razorpay/order` | POST | Create Razorpay order |
| `/payments/razorpay/webhook` | POST | Razorpay webhook handler |
| `/referral/code` | GET | Get/generate referral code + link |
| `/referral/stats` | GET | Referral stats (total, credits) |

### Admin API (`/auth/api/v1/admin/`) — requires super-admin role

| Path | Method | Purpose |
|------|--------|---------|
| `/admin/dashboard/stats` | GET | Platform-wide aggregate stats |
| `/admin/dashboard/roles` | GET | List all available roles |
| `/admin/users` | GET | List all users (paginated, cross-tenant) |
| `/admin/users/{userId}` | GET | User detail + roles + wallet |
| `/admin/users/{userId}/enable` | POST | Re-enable a disabled user |
| `/admin/users/{userId}/suspend` | POST | Suspend (disable) a user |
| `/admin/users/{userId}` | DELETE | Delete user from DB |
| `/admin/users/{userId}/roles` | POST | Assign role to user |
| `/admin/users/{userId}/roles/{roleId}` | DELETE | Remove role from user |
| `/admin/credits/wallets/{userId}` | GET | View user wallet |
| `/admin/credits/wallets/{userId}/transactions` | GET | Transaction history |
| `/admin/credits/wallets/{userId}/grant` | POST | Admin grant credits |
| `/admin/plans` | GET | List all plans (incl. inactive) |
| `/admin/plans` | POST | Create new plan |
| `/admin/plans/{planId}` | PUT | Update plan |
| `/admin/plans/{planId}` | DELETE | Soft-deactivate plan |
| `/admin/bootstrap` | POST | Link Cognito sub to seeded admin (internal) |

### Backend Service (`/api/v1/`)

| Path | Method | Purpose |
|------|--------|---------|
| `/hello` | GET | Health check |
| `/ledgers/upload` | POST | Upload Excel ledger files (multipart) |
| `/rule37/runs` | GET | List calculation runs (paginated) |
| `/rule37/runs/{id}` | GET | Get run with full calculation data |
| `/rule37/runs/{id}` | DELETE | Delete run |
| `/rule37/runs/{id}/export` | GET | Export run to Excel |

### Gateway Config

| Path | Method | Purpose |
|------|--------|---------|
| `/api/config/cognito` | GET | Cognito config for frontend |

## Database Schema

### Auth Service Tables
- `users` — user registry (Cognito sub as PK, tenant_id discriminator)
- `roles` — predefined roles (super-admin, admin, editor, viewer, guest) with scope (PLATFORM/TENANT)
- `user_roles` — user-role assignments
- `permissions` — fine-grained permissions (entry:read, user:invite, etc.)
- `role_permissions` — role-permission mappings
- `invitations` — invitation tokens with status and expiry
- `group_role_mappings` — SSO group-to-role mappings
- `acl_entries` — resource-level ACLs (Google Drive-style sharing)
- `plans` — credit pricing plans (trial: 2 credits/₹0, pro: 5/₹1000, ultra: 30/₹3000)
- `user_credit_wallets` — per-user credit balance with optimistic locking
- `credit_transactions` — immutable credit transaction ledger
- `referrals` — referral codes and conversion tracking for credit rewards

### Backend Service Tables
- `rule37_calculation_runs` — Rule 37 results (JSONB for LedgerResult[], auto-expiry via RetentionScheduler)

## Environment Variables

| Variable | Used By | Source |
|----------|---------|--------|
| `COGNITO_USER_POOL_ID` | Gateway, Auth | SSM / .env |
| `COGNITO_CLIENT_ID` | Auth | SSM / .env |
| `COGNITO_CLIENT_SECRET` | Auth | SSM / .env |
| `COGNITO_ISSUER_URI` | Gateway, Auth | SSM / .env |
| `SPRING_DATASOURCE_URL` | All services | docker-compose / SSM |
| `SPRING_DATASOURCE_USERNAME` | All services | docker-compose / Secrets Manager |
| `SPRING_DATASOURCE_PASSWORD` | All services | docker-compose / Secrets Manager |
| `REDIS_HOST` | Gateway, common-infra | docker-compose / SSM |
| `REDIS_PORT` | Gateway, common-infra | docker-compose / SSM |
| `EUREKA_URI` | All services | docker-compose |
| `RAZORPAY_KEY_ID` | Auth | .env |
| `RAZORPAY_KEY_SECRET` | Auth | .env |
| `RAZORPAY_WEBHOOK_SECRET` | Auth | .env |
| `AWS_REGION` | Auth, Lambdas | .env / SSM |
| `APP_REFERRAL_REWARD_CREDITS` | Auth | .env (default: 2) |
| `APP_REFERRAL_BASE_URL` | Auth | .env (default: localhost) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | All services | docker-compose |
| `APP_RETENTION_DAYS` | Backend | .env (default: 7) |

## External Integrations

| Integration | Purpose | Module |
|-------------|---------|--------|
| AWS Cognito | Identity, OAuth2, MFA | auth-service, gateway, lambdas |
| AWS SES | Email verification | auth-service |
| AWS SSM Parameter Store | Configuration management | all services |
| AWS Secrets Manager | DB credentials | terraform/RDS module |
| Razorpay | Payment processing (INR) | auth-service |
| OpenTelemetry / AWS X-Ray | Distributed tracing | all services |
| AWS Amplify | Frontend hosting + CI/CD | frontend |

## Key Architectural Decisions

1. **Multi-tenancy:** Shared database with `tenant_id` discriminator column — simpler than schema-per-tenant, sufficient for current scale
2. **Gateway-as-Gatekeeper:** Only the gateway validates JWTs; internal services trust enriched headers
3. **Service discovery:** Eureka for dynamic service location (vs. static URLs)
4. **Credit system:** Immutable transaction ledger with optimistic locking — no subscriptions, pay-per-use model
5. **Signup pipeline:** Action-based pipeline pattern (`CreateCognitoUserAction` → `GrantTrialCreditsAction` → `SendVerificationEmailAction`)
6. **Cognito Lambda triggers:** PreTokenGeneration injects selected tenant_id into JWT; PostConfirmation sets initial attributes
7. **Excel parsing:** Apache POI for ledger upload; FIFO-based payment matching for Rule 37 calculations
8. **IaC:** 100% Terraform-managed AWS infrastructure across 12 reusable modules
9. **Caching:** Two-level — Caffeine (local, fast) + Redisson (distributed, consistent)
10. **Resilience:** Circuit breakers (Resilience4j) on all inter-service and external calls

## Known Constraints / Tech Debt

- **2026-01-22:** Missing docs: `IMPLEMENTATION_PLAN.md`, `docs/PHASE1_LLD.md`, `docs/STATUS.md` (referenced in HLD but not created)
- **2026-01-22:** `PROJECT_INDEX.md` data-flow and env vars sections were incomplete (now filled)
- **2026-02-21:** Frontend environment configs have hardcoded Cognito IDs pointing to a specific user pool
- **2026-02-22:** Admin panel fully implemented — backend RBAC + API (29 unit tests), frontend guard/routing/5 pages (64 unit tests)
- **2026-02-21:** System tests skipped by default; no CI gate enforcing them
- **2026-02-21:** `.github/copilot-instructions.md` is empty
- **2026-02-21:** Backend service uses 7-day default retention with `RetentionScheduler` — no user-configurable retention yet
