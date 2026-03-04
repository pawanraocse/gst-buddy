# AGENT_MEMORY
_Source of truth for long-term project knowledge._
_Last updated: 2026-03-03 | Updated by: Antigravity_

---

## Project Identity
- **Name:** gst-buddy
- **Type:** GST management software
- **Owner:** [team or person]
- **Repo:** [repo URL or path]
- **Status:** [alpha / beta / production]

## Architecture Overview
- Fully decoupled foundation for multi-tenant SaaS.
- Database-per-tenant isolation strategy.
- Terraform-managed infrastructure across environments.

## Key Modules & Responsibilities
| Module | Path | Responsibility | Owner |
|--------|------|----------------|-------|
| Gateway | gateway-service/ | API Gateway, Routing | |
| Auth | auth-service/ | Authentication, Cognito Integration | |
| Backend | backend-service/ | Domain Logic, Tenant routing | |
| Frontend | frontend/ | Angular SPA | |

## Established Patterns & Conventions
- Uses Spring Boot 3.5.9 and Java 21, along with Angular 21.

## Known Constraints & Non-Negotiables
- Strict decoupling: business logic separate from infrastructure tasks.

## External Dependencies (critical ones only)
| Package | Version | Why used | Risk |
|---------|---------|----------|------|
| ...     | ...     | ...      | ...  |

## Gotchas & Hard-Won Lessons
- [Date] [CONFIDENCE] — [lesson learned]
- ...

## Active Context (current sprint / focus)
- Setup persistent memory system
