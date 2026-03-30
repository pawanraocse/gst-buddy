# DECISIONS
_Immutable record of WHY things are built the way they are._

---

## ADR-001 | 2026-03-03 | [HIGH]
**Title:** Persistent Memory Initialization
**Status:** Accepted
**Context:** To maintain robust context and strict cross-session continuity when transitioning across AI agents.
**Decision:** Adopted `.memory` based schemas across `.cursorrules` and `.agentskills`.
**Consequences:** Agents must perform the boot sequence to read, and at the end write back, any contextual knowledge and decisions generated.
**Alternatives Considered:** None

---

## ADR-002 | 2026-03-07 | [HIGH]
**Title:** Bootstrap Script DB Fallback
**Status:** Accepted
**Context:** The bootstrap API call (`/auth/api/v1/admin/bootstrap/new-super-admin`) can fail due to gateway security, service not ready, or network issues. When it fails, the admin user exists in Cognito but has no DB record/roles, making the admin panel unusable (500 errors).
**Decision:** Added direct DB fallback to `bootstrap-system-admin.sh`. If API returns non-200, the script SSHs into EC2 bastion and directly inserts user + super-admin role + credit wallet via psql. Also cleans up the `SYSTEM_ADMIN_PLACEHOLDER` seeded record.
**Consequences:** Script requires SSH key access to EC2. DB schema changes to `users`, `user_roles`, or `user_credit_wallets` must also update the fallback SQL in the script.
**Alternatives Considered:** Manual DB insertion (error-prone), making the API idempotent with retries (doesn't solve auth issues)

---

## ADR-003 | 2026-03-07 | [MEDIUM]
**Title:** Dual Security Config for Public Endpoints
**Status:** Accepted
**Context:** Adding a public/internal endpoint (like bootstrap) requires updating security in TWO places: (1) Gateway `SecurityConfig.java` — so the gateway doesn't require a JWT, and (2) Auth-service `SecurityConfiguration.java` — so Spring Security doesn't require OAuth2 auth.
**Decision:** Documented this as a gotcha in `.memory/AGENT_MEMORY.md` and `AGENTS.md`. Both files must be updated when adding new public endpoints.
**Consequences:** Missing either config causes 401 errors that are hard to debug (the error doesn't say which layer rejected the request).
**Alternatives Considered:** Single security config (not possible with gateway + service architecture)

---

## ADR-004 | 2026-03-07 | [LOW]
**Title:** DB Tunnel Scripts with Shell Aliases
**Status:** Accepted
**Context:** RDS is in a private subnet, accessible only via EC2 bastion. Developers need a quick way to connect pgAdmin or other DB clients.
**Decision:** Created `scripts/{budget,production}/db-tunnel.sh` that auto-fetch connection details from SSM

and open SSH tunnels. Added `sshgstbudget` (port 5433) and `sshgstprod` (port 5434) aliases to `~/.zshrc`.
**Consequences:** Aliases are user-specific (in `~/.zshrc`). Other team members need to add them manually.
**Alternatives Considered:** pgAdmin with built-in SSH tunnel config (requires manual setup per user)

---

---

## ADR-005 | 2026-03-29 | [CRITICAL]
**Title:** Mandatory Planning & Approval Workflow
**Context:** Prevent accidental or premature deployments to the 'prod' branch or infrastructure changes.
**Decision:** All non-trivial actions (commits, merges, terraform, docker) MUST follow the Planning Mode workflow (Research -> Plan -> Approval -> Task -> Execute -> Verify).
**Consequences:** Any agent session MUST present a plan and wait for "APPROVED" before touching production resources.
**Alternatives Considered:** None (Required for safety)
