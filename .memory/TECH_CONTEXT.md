# TECH_CONTEXT
_Living document. Update when stack or conventions change._
_Last updated: 2026-03-07_

## Stack
- **Language:** Java 21, TypeScript 5.9
- **Runtime:** JVM (Spring Boot 3.5.9), Node.js (Angular 21)
- **Framework:** Spring Cloud 2025.0.0 (Gateway, Eureka), PrimeNG 21, PrimeFlex 4
- **Auth SDK:** AWS Amplify 6.15.8 (frontend), AWS Cognito SDK (backend)
- **Database:** PostgreSQL 16 — shared schema, `tenant_id` discriminator (NOT database-per-tenant)
- **Cache:** Caffeine (local L1) + Redisson/Redis (distributed L2)
- **Payments:** Razorpay SDK 1.4.6 (INR)
- **Observability:** OpenTelemetry 1.43.0, Micrometer, AWS X-Ray
- **Testing:** JUnit 5, Mockito, AssertJ, Testcontainers, REST Assured, Jasmine + Karma
- **IaC:** Terraform 1.9+
- **CI/CD:** GitHub Actions → ECR → ECS (production); rsync to EC2 (budget)
- **DB Migrations:** Flyway (per-service history tables)

## Coding Conventions

### Java
- Controller → Service → Repository (no ServiceImpl interfaces unless needed)
- `@RequirePermission(resource, action)` for admin endpoints — enforced by `AuthorizationAspect`
- Signup uses pipeline pattern: chain of `SignupAction` implementations
- Export uses strategy pattern: `ExportStrategy` interface
- Context paths: auth-service = `/auth`, backend-service = none
- Every entity has `tenant_id` — `TenantFilter` auto-scopes queries

### Angular
- Feature modules: `features/{auth,dashboard,admin,rule37}/`
- `BaseApiService` for CRUD, `HttpClientService` with auth interceptor
- Guards: `authGuard`, `guestGuard`, `adminGuard` (checks `custom:role`)
- Env config generated at build time: `scripts/generate-env.js`

### Terraform
- Modules in `terraform/modules/` (kebab-case)
- Environments in `terraform/envs/{budget,production}/`
- Shared vars in `terraform/common.auto.tfvars`
- SSM for config, Secrets Manager for DB passwords

### Scripts
- `set -euo pipefail` on every script
- Auto-detect SSH key from `~/.ssh/`
- Fetch all dynamic config from SSM (never hardcode IPs/passwords)

## Test Requirements
- Unit tests for all business logic and controllers
- Integration tests via Testcontainers (Postgres + Redis)
- System tests in `system-tests/` (REST Assured) — currently skipped in CI

## Build Commands
```bash
# Local dev
docker-compose up -d

# Budget deploy
./scripts/budget/deploy.sh

# Production deploy
./scripts/production/deploy.sh

# Run tests
mvn test                    # unit tests
mvn verify -Pit             # integration tests
cd frontend && npm test     # Angular tests
```
