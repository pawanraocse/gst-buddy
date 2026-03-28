# SESSION_LOG
_Last 10 sessions. Oldest sessions pruned when limit exceeded._

---

## Session: 2026-03-28 21:15 | Agent: Antigravity (Gemini)

### What Was Done
- **Fixed Production Bug: 0 Credits**: Resolved critical prod gap where Google SSO users had 0 credits.
- **Backend Code Fix**: Updated `AuthServiceImpl.java` to grant 2 trial credits during `getCurrentUser` flow if wallet is missing.
- **ID Consistency**: Fixed `GrantTrialCreditsAction.java` and internal logic to use Cognito `sub` (UUID) as the primary key for wallets instead of email.
- **Production Seeding**: Seeded `plans` table in RDS with 'trial' plan (2 credits).
- **Cognito Fix**: Updated `terraform/modules/cognito-user-pool/main.tf` to map `email_verified` from Google Identity Provider.
- **CORS Fix**: Updated `ssm_frontend.tf` and `start.sh` to include `https://gstbuddies.com` in allowed origins.
- **Manual Patch**: Restored 2 credits to `pawan.weblink@gmail.com` in production via manual SQL.
- **Redeployed**: Rebuilt and deployed `auth-service` to `prod_init`.

### Files Changed
- `auth-service/src/main/java/com/learning/authservice/auth/service/AuthServiceImpl.java`
- `auth-service/src/main/java/com/learning/authservice/signup/actions/GrantTrialCreditsAction.java`
- `terraform/modules/cognito-user-pool/main.tf`
- `terraform/envs/prod_init/ssm_frontend.tf`
- `scripts/prod_init/start.sh`
- `scripts/bootstrap-system-admin.sh`

### Decisions
## Stack
- **Language:** Java 21, TypeScript 5.9
- **Runtime:** JVM (Spring Boot 3.5.9), Node.js (Angular 21)
- **Framework:** Spring Cloud 2025.0.0 (Gateway, Eureka), PrimeNG 21, PrimeFlex 4
- **Auth SDK:** AWS Amplify 6.15.8 (frontend), AWS Cognito SDK (backend)
- **Database:** PostgreSQL 16 — shared schema, `tenant_id` discriminator (NOT database-per-tenant)
- **IdP:** AWS Cognito with Google/SAML Social Federation
- **Cache:** Caffeine (local L1) + Redisson/Redis (distributed L2)
- **Payments:** Razorpay SDK 1.4.6 (INR)
- **Observability:** OpenTelemetry 1.43.0, Micrometer, AWS X-Ray
- **Testing:** JUnit 5, Mockito, AssertJ, Testcontainers, REST Assured, Jasmine + Karma
- **IaC:** Terraform 1.9+
- **CI/CD:** GitHub Actions → ECR → ECS (production); rsync to EC2 (prod_init/budget)
- **DB Migrations:** Flyway (per-service history tables)
- Verify GSTR-2A/2B reconciliation logic under the new UUID-based wallet system.

---

## Session: 2026-03-28 17:19 | Agent: Antigravity

### What Was Done
- Fixed GitHub Actions `deploy-production.yml` matrix context syntax error.
- Fixed `deploy-production.yml` Maven build context to resolve inter-module dependencies from root.
- Removed `otel-collector` completely from all docker-compose and deployment scripts to eliminate cost/burden.

### Files Changed
- `.github/workflows/deploy-production.yml` — fixed CI/CD logic
- `docker-compose.base.yml` — removed otel-collector service
- `docker-compose.yml` — removed otel-collector mapping
- `terraform/envs/budget/main.tf` — removed otel ECR repo
- `scripts/budget/destroy.sh` — removed from cleanup list
- `.memory/SESSION_LOG.md` — updated session log

### Decisions Made
- Omitted standalone `otel-collector` from all environments due to infrastructure overhead. Will pursue ADOT sidecar pattern when tracing is required natively in ECS.

### Carry-Forward
- Wait for team alignment before implementing ADOT sidecars in ECS task definitions.

---

## Session: 2026-03-07 15:04 | Agent: Antigravity

### What Was Done
- Ran `bootstrap-system-admin.sh` to create admin (`pawan.weblink@gmail.com`) for budget env
- Diagnosed 401 on bootstrap Step 4 (DB linking failed)
- Fixed auth-service `SecurityConfiguration.java`: `/api/v1/admin/bootstrap` → `/api/v1/admin/bootstrap/**`
- Fixed gateway `SecurityConfig.java`: added `/auth/api/v1/admin/bootstrap/**` to permitAll
- Fixed bootstrap script URL routing (gateway path vs local path)
- Diagnosed 500 on `/admin/users` — admin user had no `super-admin` role in DB
- Connected to budget RDS via SSH bastion, inserted `super-admin` role and updated user name
- Cleaned up `SYSTEM_ADMIN_PLACEHOLDER` from database
- Enhanced `bootstrap-system-admin.sh` with direct DB fallback mechanism
- Added `ENVIRONMENT` as optional 3rd argument to bootstrap script
- Created `scripts/budget/db-tunnel.sh` (localhost:5433) and `scripts/production/db-tunnel.sh` (localhost:5434)
- Added `sshgstbudget` and `sshgstprod` aliases to `~/.zshrc`
- Rewrote `scripts/README.md` with full documentation
- Created `AGENTS.md` at project root for AI agent context
- Updated all `.memory/` files with current project state

### Files Changed
- `auth-service/src/main/java/.../config/SecurityConfiguration.java` — bootstrap wildcard
- `gateway-service/src/main/java/.../config/SecurityConfig.java` — bootstrap permitAll
- `scripts/bootstrap-system-admin.sh` — DB fallback, env arg, URL fix, placeholder cleanup
- `scripts/budget/db-tunnel.sh` — [NEW] SSH tunnel script
- `scripts/production/db-tunnel.sh` — [NEW] SSH tunnel script
- `scripts/README.md` — full rewrite
- `AGENTS.md` — [NEW] agent context file
- `~/.zshrc` — added aliases
- `.memory/*` — updated all files

### Decisions Made
- ADR-002: Bootstrap DB fallback
- ADR-003: Dual security config for public endpoints
- ADR-004: DB tunnel scripts with aliases

### Blockers / Open Questions
- Gateway + auth-service code changes need budget redeploy to take effect (bootstrap API path currently relies on DB fallback)

### Carry-Forward
- Redeploy budget after this session to activate the security config fixes
- Test bootstrap script end-to-end on fresh environment

---

## Session: 2026-03-05 15:49 | Agent: Antigravity

### What Was Done
- AI toolkit enhancement — optimized token consumption, improved response accuracy

### Files Changed
- AI toolkit scripts and skills

### Decisions Made
- None

### Carry-Forward
- Continue AI toolkit refinement

---

## Session: 2026-03-03 19:15 | Agent: Antigravity

### What Was Done
- Initialized persistent memory system across the `GSTbuddies` project

### Files Changed
- `.cursorrules` — updated agent instructions to enforce memory system
- `.agentskills` — created persistent memory trigger
- `.memory/` — created directory and base schemas

### Decisions Made
- ADR-001: Persistent Memory Initialization

### Carry-Forward
- Populate existing architectural decisions

---
