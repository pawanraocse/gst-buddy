# TODO
_Owned by the memory system. Agents update this; humans approve priorities._
_Last updated: 2026-03-28_

## 🔴 Blocked
- None

## 🟡 In Progress
- [ ] Monitor production logs for any remaining CORS preflight failures.

## 🟢 Up Next (prioritised)
- [ ] Implement GSTR-1 Late Fee Engine (PRD Ref: `resources/RULE ENGINE/LATE FEES GSTR-1/`)
- [ ] Implement Export & LUT Compliance Engine
- [ ] Implement Concessional Rate Validator
- [ ] Implement Rule 42/43 Reversal Engine
- [ ] Implement Job Work (ITC-04) Tracking Engine
- [ ] Implement GSTR-3B Interest Engine (Net Tax Basis)
- [ ] Implement GSTR-3B Late Fee Engine
- [ ] Extract Rule Engine Core from Rule 37 logic for modularity
- [ ] Verify GSTR-2A/2B reconciliation logic under the new UUID-based wallet system.
- [ ] Stripe integration (US/international payments alongside Razorpay)
- [ ] Audit logging for admin actions
- [ ] User-configurable data retention (currently hardcoded 7 days)
- [ ] Frontend environment configs — remove hardcoded Cognito IDs
- [ ] System tests CI gate enforcement

## ✅ Completed (last 2 weeks)
- [x] Fixed Production Bug: 0 Credits (Gemini) — 2026-03-28
- [x] Seeded `plans` table in RDS — 2026-03-28
- [x] Fixed Google SSO email_verified mapping in Terraform — 2026-03-28
- [x] Added custom domain to CORS origins — 2026-03-28
- [x] Budget environment deployed — 2026-03-07
- [x] Admin user bootstrapped (pawan.weblink@gmail.com) — 2026-03-07
- [x] Fixed SecurityConfiguration: /api/v1/admin/bootstrap/** wildcard — 2026-03-07
- [x] Fixed gateway SecurityConfig: bootstrap endpoint permitAll — 2026-03-07
