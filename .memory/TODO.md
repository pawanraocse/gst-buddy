# TODO
_Owned by the memory system. Agents update this; humans approve priorities._
_Last updated: 2026-03-07_

## 🔴 Blocked
- None

## 🟡 In Progress
- None currently

## 🟢 Up Next (prioritised)
- [ ] Redeploy budget with gateway + auth-service security fixes (bootstrap endpoint permitAll)
- [ ] Test production deployment end-to-end
- [ ] Stripe integration (US/international payments alongside Razorpay)
- [ ] SSO support (SAML/OIDC for enterprise tenants)
- [ ] Audit logging for admin actions
- [ ] User-configurable data retention (currently hardcoded 7 days)
- [ ] Frontend environment configs — remove hardcoded Cognito IDs
- [ ] System tests CI gate enforcement

## ✅ Completed (last 2 weeks)
- [x] Budget environment deployed — 2026-03-07
- [x] Admin user bootstrapped (pawan.weblink@gmail.com) — 2026-03-07
- [x] Fixed SecurityConfiguration: `/api/v1/admin/bootstrap/**` wildcard — 2026-03-07
- [x] Fixed gateway SecurityConfig: bootstrap endpoint permitAll — 2026-03-07
- [x] Bootstrap script: added DB fallback + env arg + placeholder cleanup — 2026-03-07
- [x] Created `db-tunnel.sh` for budget + production — 2026-03-07
- [x] Added `sshgstbudget`/`sshgstprod` aliases — 2026-03-07
- [x] Updated scripts/README.md with full documentation — 2026-03-07
- [x] Created AGENTS.md (project memory for AI agents) — 2026-03-07
- [x] AI toolkit enhancement (token optimization, response accuracy) — 2026-03-05
- [x] Persistent memory system initialized — 2026-03-03
