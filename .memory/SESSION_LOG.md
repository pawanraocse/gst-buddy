## Session: 2026-04-02 21:58 | Agent: Antigravity
### What Was Done
- **Resource Cross-Check**: Deep-dived into `resources/RULE ENGINE/` and unzipped `MASTER RULE ENGINE.docx` to extract 34+ specific rule requirements.
- **Roadmap Finalization**: Updated `docs/ROADMAP.md` to include missing **Job Work (ITC-04)** module, **Section 16(5) Amnesty** logic, and **Rule 42 share/security valuation** rules.
- **Decision Resolution**: Finalized all 5 open questions with the user, establishing a credit-based monetisation model and piecemeal return upload strategy.

### Files Changed
- `docs/ROADMAP.md` [UPDATED]
- `.memory/TODO.md` [UPDATED]

### Decisions
- Added Job Work (ITC-04) as Module 4.4.
- Confirmed Section 16(5) inclusion for historic ITC deadline extensions.
- Finalized Credit Pack pricing (Rs 199 to Rs 9,999).

---

## Session: 2026-04-01 21:40 | Agent: Antigravity
### What Was Done
- **Roadmap Planning**: Conducted deep research into `resources/RULE ENGINE/` PRDs and Excel calculators.
- **Updated ROADMAP.md**: Created a multi-phased product roadmap for expanding from Rule 37 to a full GST Audit platform.
- **Audit Rules Prioritization**: Prioritized Late fees (GSTR-1, 3B), Net liability interest, and ITC reconciliation.
- **Architecture Planning**: Proposed `AuditRule` engine abstraction to modularize backend-service.

### Files Changed
- `docs/ROADMAP.md` [UPDATED]
- `.memory/TODO.md` [UPDATED]

### Decisions
- Standardizing on `AuditFinding` object for all rules to ensure uniform reporting and chaining.
- Phase 1 focuses on high-frequency "Penalty Intelligence" for CA retention.

---

## Session: 2026-03-31 16:50 | Agent: Antigravity

### What Was Done
- **Resolved Razorpay 500 Error**: Fixed payment order creation failure caused by `receipt` ID exceeding the 40-character limit. Implemented a shortened pattern (`p_` + 8-char `userId` + `timestamp`).
- **Local Verification**: Added `RazorpayPaymentServiceTest.java` to verify the order creation payload and receipt length (24 chars) before deployment.
- **Fixed 401 Unauthorized**: Whitelisted `/api/v1/payments/**` in both `gateway-service` and `auth-service` security configurations to allow unauthenticated order creation.
- **Mumbai Environment Fixes**: Corrected hardcoded `us-east-1` region fallbacks in `auth-service` to `ap-south-1`.
- **Connectivity & Stability**: Mapped host port **80** to Gateway Service, increased health check safety margins for `t3.medium`, and codified rules in Terraform.
- **Infrastructure Persistence**: Updated `bastion` module for ports 80/443 and `prod_init` main.tf for Razorpay SSM parameters.

### Files Changed
- `auth-service/src/main/java/com/learning/authservice/credit/service/RazorpayPaymentService.java`
- `auth-service/src/test/java/com/learning/authservice/credit/service/RazorpayPaymentServiceTest.java` [NEW]
- `auth-service/src/main/java/com/learning/authservice/config/SecurityConfiguration.java`
- `gateway-service/src/main/java/com/learning/gateway/config/SecurityConfig.java`
- `auth-service/src/main/resources/application.yml`
- `docker-compose.prod_init.yml`
- `terraform/modules/bastion/main.tf`
- `terraform/envs/prod_init/main.tf`

### Decisions
- **ADR-008**: Whitelisted payment creation endpoints at Gateway and Auth levels to support public checkout flows.
- **ADR-009**: Standardized receipt ID generation to a 24-character prefixed timestamp to comply with Razorpay constraints.
- **Infrastructure**: Direct port 80 mapping on EC2 was chosen for the `prod_init` (budget) environment to minimize Load Balancer costs while maintaining CloudFront compatibility.

---

## Session: 2026-03-31 15:45 | Agent: Antigravity

### What Was Done
- **Infrastructure Migration**: Successfully migrated the `prod_init` environment from `us-east-1` to `ap-south-1` (Mumbai) to satisfy GST data residency requirements. Maintained `us-east-1` specifically for CloudFront ACM certificates.
- **State Management**: Provisioned a new S3 backend and DynamoDB lock table in `ap-south-1`.
- **Razorpay Integration**: Injected live Razorpay Key IDs and Webhook Secrets into the new Mumbai SSM Parameter Store.
- **CI/CD Fixes**: Resolved GitHub Actions deployment failures triggered by a casing mismatch (`GSTbuddies` vs `gstbuddies`) in SSM paths.
- **EC2 Bootstrap Fixes**: Modified `.github/workflows/deploy-prod-init.yml` to automatically create and chown the `/app` directory on fresh EC2 instances before rsync.
- **Amazon Linux 2023 Compatibility**: Refactored `scripts/prod_init/start.sh` to use `sudo -E` for preserving environment variables, disabled BuildKit (`DOCKER_BUILDKIT=0`), and switched from standalone `docker-compose` to native `docker compose` to resolve buildx incompatibilities.
- **Validation**: Backend successfully deployed and running on the new Mumbai EC2 instance.

### Files Changed
- `terraform/common.auto.tfvars`
- `terraform/envs/prod_init/terraform.tfvars`
- `terraform/envs/prod_init/main.tf`
- `terraform/envs/prod_init/outputs.tf`
- `.github/workflows/deploy-prod-init.yml`
- `scripts/prod_init/start.sh`
- `.memory/SESSION_LOG.md`
- `artifacts/task.md`

### Decisions
- Standardized Docker Compose execution in EC2 startup scripts using `sudo -E docker compose` and explicit BuildKit disable flags to guarantee out-of-the-box compatibility with native Amazon Linux 2023 environments.
- Enforced complete lowercasing for all dynamic SSM parameter resolutions within CI pipelines to prevent cross-environment edge cases.

---

## Session: 2026-03-30 13:35 | Agent: Antigravity (Gemini)

### What Was Done
- **Production Infrastructure Recovery**: Restored `prod_init` EC2 instance after casing mismatch in `terraform.tfvars` (`gstbuddies` vs `GSTbuddies`) caused resource destruction.
- **CI/CD Automation**: Created `.github/workflows/deploy-prod-init.yml` to automate backend builds, rsync deployment to EC2, and Amplify frontend triggers.
- **Backend Fixes**: Fixed `scripts/prod_init/start.sh` to correctly handle `PROJECT_NAME` casing when fetching AWS SSM parameters.
- **Amplify Path Fix**: Corrected `baseDirectory` in `terraform/envs/prod_init/main.tf` to `frontend/dist/gstbuddies`.
- **Validation**: Manually verified backend health and successfully triggered an Amplify release.

### Files Changed
- `.github/workflows/deploy-prod-init.yml` [NEW]
- `scripts/prod_init/start.sh`
- `terraform/envs/prod_init/terraform.tfvars`
- `terraform/envs/prod_init/main.tf`

### Decisions
- Standardized `PROJECT_NAME` as `GSTbuddies` (case-sensitive) across all Terraform and SSM logic to prevent infrastructure drift.
- Automated `prod_init` deployment on every push to the `prod` branch.

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
