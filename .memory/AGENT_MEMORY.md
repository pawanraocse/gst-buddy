# AGENT_MEMORY
_Source of truth for long-term project knowledge._
_Last updated: 2026-03-29 | Updated by: Antigravity_

---

## 🏆 OPERATIONAL SAFETY
- **CRITICAL**: No `terraform apply`, `git push origin prod`, or `docker` deployments without explicit user **"APPROVED"** on an implementation plan.
- **RESTRICTED ZONES**: `main` (branch), `prod` (branch), `terraform/` (all modules).
- **Mandatory Workflow**: Always use Planning Mode for infrastructure or production environment changes.

---

## Project Identity
- **Name:** GSTbuddies (GSTbuddies)
- **Type:** Multi-tenant SaaS for Indian GST compliance (Rule 37 ITC reversals)
- **Owner:** Pawan Yadav
- **Repo:** `~/prototype/GSTbuddies`
- **Status:** Beta — Budget env deployed, Production scaffold ready

## Architecture Overview
- Microservices: Gateway (8080) → Auth (8081) / Backend (8082), Eureka (8761)
- Shared PostgreSQL database, `tenant_id` discriminator (NOT database-per-tenant)
- AWS Cognito for identity, Razorpay for payments (INR)
- Gateway validates JWTs, injects `X-Tenant-Id`/`X-User-Id` headers — downstream trusts headers
- Terraform-managed infrastructure (12 modules)
- Budget = EC2 + Docker Compose + CloudFront; Production = ECS Fargate + ALB

## Key Modules & Responsibilities
| Module | Path | Port | Responsibility |
|--------|------|------|----------------|
| Gateway | `gateway-service/` | 8080 | JWT validation, Eureka routing, rate limiting, circuit breakers, header enrichment |
| Auth | `auth-service/` | 8081 | Cognito integration, signup pipeline, RBAC, credits, Razorpay, invitations, referrals, admin panel API |
| Backend | `backend-service/` | 8082 | Rule 37 engine, Excel upload/export, credit consumption |
| Eureka | `eureka-server/` | 8761 | Service discovery |
| Common DTO | `common-dto/` | — | Shared DTOs, enums, constants |
| Common Infra | `common-infra/` | — | TenantFilter, caching, `@RequirePermission`, distributed locking |
| Frontend | `frontend/` | 4200 | Angular 21 SPA, PrimeNG, AWS Amplify auth |

## Key Reference Files
- `AGENTS.md` — Full agent context (architecture + conventions + state)
- `PROJECT_INDEX.md` — API endpoints, DB schema, env vars
- `HLD.md` — Architecture diagram, doc links
- `project.config` — Central naming/config
- `scripts/README.md` — All scripts documented

## Gotchas & Hard-Won Lessons
- 2026-03-07 [HIGH] — Gateway `SecurityConfig.java` AND auth-service `SecurityConfiguration.java` BOTH need updating when adding public/internal endpoints. Missing either causes 401.
- 2026-03-07 [HIGH] — Spring Security `requestMatchers("/path")` does NOT match sub-paths. Use `/path/**` for wildcard matching.
- 2026-03-07 [MEDIUM] — The Cognito PostConfirmation Lambda creates a user record in the DB on first login, but does NOT assign roles. Admin roles must be assigned via the bootstrap script/API.
- 2026-03-07 [MEDIUM] — Gateway routes: `/auth/**` preserves host header; `/auth-service/**` rewrites path. Use `/auth/api/v1/...` for API calls through gateway.
- 2026-03-07 [LOW] — Budget RDS is in a private subnet. Access via SSH tunnel through EC2 bastion only.

## Active Context
- Budget environment deployed and operational
- Admin user bootstrapped: `pawan.weblink@gmail.com` (super-admin)
- Bootstrap script hardened with DB fallback
- DB tunnel scripts + aliases created

## PATTERNS

---

## Pattern: Grant-on-Login (2026-03-28)
- **Description**: Automatically grants baseline resources (like trial credits) during the auth session retrieval.
- **Implementation**: `AuthServiceImpl.getCurrentUser()` calls `creditService.grantTrialCredits()`.
- **Benefit**: Handles social/SSO users who don't hit "Signup Complete" screens.

## Pattern: UUID-to-Email Alias (2026-03-28)
- **Description**: Keying internal resources to Cognito `sub` but allowing lookup by `email`.
- **Implementation**: `users` table maps both; `user_credit_wallets` relies exclusively on `user_id` (UUID).

## Current State & Findings (2026-03-28)
- **Production Environment**: `prod_init` (EC2 + RDS) is the current target.
- **Critical Bug**: Google SSO users had 0 credits because they bypassed the signup pipeline.
- **Root Cause 1**: `GrantTrialCreditsAction` used `email` instead of Cognito `sub` (UUID) as `userId`.
- **Root Cause 2**: SSO login skipped the `SignupAction` chain.
- **Fix**: Added "grant-on-login" logic in `AuthServiceImpl`.
- **CORS**: `https://gstbuddies.com` wasn't in allowed origins; updated SSM and start scripts.
- **Cognito**: Google identity provider was missing `email_verified` mapping. Fixed in Terraform.
- **Database**: `plans` table was empty in production; seeded manually with 'trial' plan.
