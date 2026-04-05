# GSTbuddies — Product Roadmap
**From Rule 37 MVP to India's Most Complete GST Audit Intelligence Platform**
_Last updated: 2026-04-05 · Author: Antigravity (GST Expert + Senior Architect) · Status: APPROVED_

---

> [!IMPORTANT]
> **MVP Deployed**: Rule 37 (ITC reversal on 180-day non-payment — Section 16(2), Rule 37 CGST Rules) is live in production (Mumbai ap-south-1).
> This roadmap covers **18 audit modules** across 5 phases over 15 months, covering every major GST compliance risk a CA faces.

---

## 🏛️ Strategic Vision

GSTbuddies will become the **Single Source of Truth** for GST audits — a legal-compliance shield that surfaces every rupee of risk, penalty, and missed credit **before** the tax officer does.

### Core Pillars
1. **Legal Precision**: Every finding cites exact Section/Rule/Notification — never vague "as per GST law".
2. **Audit Reliability**: All calculations validated against CBIC official offline utilities and SOPs before release.
3. **Frictionless Compliance**: Automate reconciliation of Returns (GSTR-1, 3B, 9, 2A/2B) vs. Internal Books.
4. **Zero False Negatives**: A missed CRITICAL finding destroys CA trust permanently — conservative by default.

### Trust-Building Commitments (Non-Negotiable)
- Every calculation is **fully auditable** — formula, inputs, and day-count breakdown shown.
- CBIC relief/amnesty windows are **DB-driven** — updated within 48h of gazette notification.
- Every new rule engine **validated by a practising CA** before production release.

---

## 🏗️ Phase 0: Foundation (COMPLETED)
**Status**: Live in Production

| Capability | Status |
|---|---|
| Excel ledger ingestion & parsing | ✅ Done |
| Rule 37 ITC 180-day reversal engine | ✅ Done |
| Credit consumption & wallet system | ✅ Done |
| Multi-tenant architecture (tenant_id) | ✅ Done |
| Export to Excel (Strategy pattern — Rule 37 + GSTR-3B Summary) | ✅ Done |
| Razorpay payment integration (INR credit purchases) | ✅ Done |
| RBAC + Admin panel (roles, permissions, super-admin bootstrap) | ✅ Done |
| Referral system | ✅ Done |
| Trial credit auto-grant (Grant-on-Login — covers signup + SSO) | ✅ Done |
| Data retention & auto-cleanup (configurable `RetentionScheduler`) | ✅ Done |
| AWS prod deployment (Mumbai, ap-south-1) | ✅ Done |
| CI/CD via GitHub Actions (EC2 deploy + Amplify frontend) | ✅ Done |

**⏳ Gap to fill before Phase 1 (NOT STARTED)**: Extract a unified `AuditRule<I, O>` interface + `AuditRuleRegistry` + `AuditFinding` DTO from the Rule 37 codebase. All future modules implement this contract.

---

## 🏗️ Phase 1: Compliance Calendar & Penalty Intelligence (Q2 2026)
**Goal**: Make GSTbuddies the first tool CAs open each month. 4 modules.

### 1.1 GSTR-1 Late Fee Calculator
**Legal Basis**: Section 47(1), CGST Act 2017
**PRD Ref**: `resources/RULE ENGINE/LATE FEES GSTR-1/` (PRD + Calculator + SOP + CBIC relief PDF)
**Business Value**: 🔴 CRITICAL — every CA needs this monthly

**Rule Logic**:
- Input: GSTR-1 filing date, original due date, aggregate turnover, return type (Nil/Non-Nil)
- Nil return: Rs 20/day (Rs 10 CGST + Rs 10 SGST), capped at Rs 500
- Non-nil return: Rs 50/day (Rs 25 CGST + Rs 25 SGST), capped at Rs 10,000
- Apply CBIC amnesty/relief notification windows (COVID waivers, QRMP extensions) — already modelled in provided Excel engine
- QRMP quarterly filers: different due dates (13th of month following quarter)
- Output: Day-count breakdown + CGST late fee + SGST late fee + total

**Engineering**: Relief windows stored in `late_fee_relief_windows` DB table (not hardcoded). Flyway migration to seed historical windows.

---

### 1.2 GSTR-3B Late Fee Calculator
**Legal Basis**: Section 47(2), CGST Act 2017
**PRD Ref**: `resources/RULE ENGINE/LATE FEES GSTR-3B/` (PRD + Calculator + SOP + CBIC relief PDF)
**Business Value**: 🔴 CRITICAL — 3B is the monthly summary return; penalties compound fast

**Rule Logic**:
- Same fee structure as GSTR-1 but **different due dates** based on turnover and state:
  - Monthly filers: 20th of next month
  - QRMP filers: 22nd or 24th (state-dependent)
- Nil return: Rs 20/day capped at Rs 500
- Non-nil return: Rs 50/day capped at Rs 10,000
- Composition dealers (GSTR-4): separate due date and fee structure
- CBIC conditional waiver notification lookup

**Key Edge Cases**:
- State-wise due date variation (22nd vs. 24th for QRMP — Notification 76/2020-CT)
- Interest under Section 50 runs **parallel** to late fee — do NOT conflate
- COVID-period waivers (Notification 19/2021-CT series)

---

### 1.3 GSTR-3B Interest on Late Tax Payment
**Legal Basis**: Section 50(1), CGST Act 2017; Notification 63/2020-CT (net liability basis)
**PRD Ref**: `resources/RULE ENGINE/Interest late filing GSTR3B/` (PRD + Excel engine)
**Business Value**: 🔴 CRITICAL — most common audit trigger; appears in nearly every ASMT-10

**Rule Logic**:
- 18% p.a. simple interest on **net cash tax liability** (post-Notification 63/2020)
- Pre-2020: interest was on gross liability — handle FY-aware switching
- Day count: from due date to actual payment date (per CBIC SOP methodology)
- Separate calculation per tax head: IGST / CGST / SGST
- Special **24% rate** for wrongfully availed AND utilised ITC (Section 50(3))
- Interest on **self-assessed tax** vs. **demand-determined tax** — different treatment

**Engineering**: Reuse `Rule37InterestCalculator` pattern. Add `InterestCalculationType` enum: `LATE_PAYMENT | EXCESS_ITC | RULE_37_REVERSAL`.

---

### 1.4 GSTR-9 / 9C Late Fee Calculator
**Legal Basis**: Section 47(2); Notification 07/2023-CT (reduced late fees)
**PRD Ref**: `resources/RULE ENGINE/GSTR 9 and 9C Late Fess/` (Logic.docx + Calculator)
**Business Value**: 🟡 HIGH — annual return deadline creates panic for every CA

**Rule Logic**:
- GSTR-9: Rs 200/day (Rs 100 CGST + Rs 100 SGST), max **0.5% of turnover in State/UT** (0.25% CGST + 0.25% SGST)
- GSTR-9C (self-certified reconciliation): same structure, filed along with GSTR-9
- Amnesty scheme: reduced cap of Rs 20,000 for FY 2017-18 to FY 2021-22 (Notification 07/2023-CT)
- Exemption: turnover up to Rs 2 crore exempted from GSTR-9 (Notification 77/2020-CT)
- GSTR-9C mandatory only for turnover > Rs 5 crore

---

## 🔍 Phase 2: Cross-Return Reconciliation & ITC Intelligence (Q3 2026)
**Goal**: Detect the discrepancies that trigger ASMT-10 / DRC-01 notices. 4 modules.

### 2.1 GSTR-1 vs GSTR-3B vs GSTR-9 Reconciliation
**Legal Basis**: Section 75(12); Circular 183/15/2022; Rule 88C; GSTR-9 Tables 4, 5, 6, 10, 11
**PRD Ref**: `resources/RULE ENGINE/1 vs 3B vs 9/` (PRD to SD)
**Business Value**: 🔴 CRITICAL — #1 audit trigger in GST; ASMT-10 notices issued routinely on this

**Rule Logic (3-level reconciliation)**:

| Level | What | Finding | Severity |
|---|---|---|---|
| L1 | GSTR-1 outward supply vs GSTR-3B Table 3.1 | Tax in GSTR-1 but understated in 3B | 🔴 CRITICAL — pay diff + 18% interest |
| L1 | GSTR-1 vs GSTR-3B | Tax in 3B but not in GSTR-1 | 🟡 HIGH — phantom liability / filing error |
| L2 | GSTR-3B (12 months) vs GSTR-9 Table 4/5 | Outward taxable value delta | 🟡 HIGH — disclose in GSTR-9C |
| L3 | GSTR-9 vs audited financials (9C) | Turnover or ITC delta | 🟡 HIGH — demand exposure |

**Rule 88C (DRC-01B auto-notice)**: The government portal now auto-generates DRC-01B when GSTR-1 > GSTR-3B liability. Our engine should predict this BEFORE the notice issues.

---

### 2.2 ITC Reconciliation (GSTR-2B vs Purchase Register)
**Legal Basis**: Section 16(2)(aa); Rule 36(4); Circular 170/02/2022
**PRD Ref**: `resources/RULE ENGINE/ITC RECO/` (126KB PRD — the most detailed document in resources)
**Business Value**: 🔴 CRITICAL — directly linked to rupee ITC claims and reversal demands

**Rule Logic (3-way match)**:
1. **Normalise**: GSTIN + Invoice No (strip spaces/special chars) + Invoice Date (±3 days fuzzy) + Taxable Value (±Rs 1 tolerance)
2. **Match** GSTR-2B JSON vs Purchase Register
3. **Classify** each invoice:
   - `MATCHED` ✅ — claim as-is
   - `IN_2A_NOT_2B` ⚠️ — supplier filed late; check subsequent month's 2B
   - `IN_BOOKS_NOT_2B` ❌ — do NOT claim; follow up with supplier or reverse
   - `RATE_MISMATCH` 🔴 — different tax rate applied; supplier correction needed
   - `CANCELLED_IN_2B` 🚫 — supplier cancelled; reverse ITC immediately
   - `INELIGIBLE_17_5` 🚫 — blocked credit under Section 17(5)
4. **Rule 36(4)**: ITC cannot exceed GSTR-2B matched amount (pre-Jan 2022: 105%/110%/120% caps)
5. **Section 17(5) block credit filter**: motor vehicles, food/beverages, club membership, etc.

**Engineering**: Dedicated `ItcReconciliationEngine`. Fuzzy matching via Levenshtein distance. Must handle 100,000+ invoices in < 60 seconds (parallel streams + batch DB writes).

---

### 2.3 RCM Reconciliation in GSTR-3B
**Legal Basis**: Section 9(3) & 9(4), CGST Act 2017; Notification 13/2017-CT (Rate), 10/2017-IT
**PRD Ref**: `resources/RULE ENGINE/RCM reco in 3B/` (PRD)
**Business Value**: 🟡 HIGH — RCM liability is the most frequently missed item, especially by SMEs

**Rule Logic**:
- Input: Purchase register with RCM-applicable vendor categories tagged
- **RCM-applicable services** (Sec 9(3)): GTA (5%), Legal services, Director sitting fees, Security services, Import of services, Sponsorship, Renting by unregistered person
- Verify: RCM liability declared in GSTR-3B Table 3.1(d) == computed from purchase register
- Verify: RCM ITC claimed in Table 4A(3) — eligible only after payment to supplier
- Flag: RCM tax must be paid through **cash ledger only** (not ITC) — Section 49(4)
- Check: Self-invoices issued for RCM purchases (required under law but commonly skipped)

---

### 2.4 Late Reporting of Invoice in GSTR-1 (Interest & Penalty)
**Legal Basis**: Section 50(1); Section 37(3) — time limit for amendments; Circular 26/26/2017-GST
**PRD Ref**: `resources/RULE ENGINE/Late reporting of invoice in GSTR1 interest and penalty/` (PRD)
**Business Value**: 🟡 HIGH — common in amended/missed invoices

**Rule Logic**:
- Invoice date falls in Period X, but reported in GSTR-1 of Period Y (Y > X)
- Tax liability arose in Period X per time of supply rules (Section 12/13)
- Interest: 18% p.a. from due date of Period X's GSTR-3B to actual payment date in Period Y
- Compute: (days delayed × tax amount × 18%) / 365, per tax head (IGST/CGST/SGST)
- Flag: if reporting was beyond Section 16(4) / 34(2) amendment window — penalty exposure

---

## 🛡️ Phase 3: Supplier Risk & ITC Quality Engine (Q4 2026)
**Goal**: Prevent ITC losses proactively, not reactively. 5 modules.

### 3.1 Supplier Compliance Risk Scoring
**Legal Basis**: Section 16(2)(c); Rule 86A (provisional ITC block); CBIC circulars on fake invoices
**PRD Ref**: `resources/RULE ENGINE/GSTR 2A/Supplier Risk/` (PRD)
**Business Value**: 🔴 CRITICAL — GSTIN suspension/cancellation cascades to buyer's ITC reversal

**Risk Signals (per supplier GSTIN)**:

| Signal | Weight | Source |
|---|---|---|
| GSTR-3B filing regularity (% of months filed in FY) | 30% | GSTR-2A filing timestamps |
| GSTR-1 vs GSTR-3B liability match | 20% | Cross-return delta |
| ITC:Turnover ratio (>95% = suspicious) | 20% | GSTR-3B Tables |
| GSTIN status (Active/Suspended/Cancelled) | 30% | GSTIN validation API |

**Output**: Score 0–100, Risk Band (LOW/MEDIUM/HIGH/CRITICAL), recommended action per band.
**Engineering**: Cache GSTIN status (TTL: 24h). Expose as shared internal API consumed by all modules. Alert CA when supplier crosses from MEDIUM → HIGH mid-year.

---

### 3.2 Section 16(4) ITC Eligibility Deadline Guard
**Legal Basis**: Section 16(4) CGST Act (as amended by Budget 2024 — extended to 30-Nov)
**PRD Ref**: `resources/RULE ENGINE/GSTR 2A/16(4) violation/` (PRD)
**Business Value**: 🔴 CRITICAL — ITC lost after this date is permanent and irrecoverable

**Rule Logic**:
- For FY X-Y: ITC can be claimed up to GSTR-3B of September (Y) **or** Annual return date, whichever is earlier
- Post-Budget 2024: extended to **30th November** of FY+1.
- **Section 16(5) Amnesty**: Handle special extension of ITC deadline for FY 2017-18, 18-19, 19-20, and 20-21 (up to 30-Nov-2021) per Budget 2024.
- Flag invoices where: invoice date FY ≠ claim FY AND claim is beyond the 30-Nov cutoff.
- Compute: irreversible ITC loss amount with **supplier-wise breakdown**.
- Proactive: daily alerts as deadline approaches for unclaimed eligible ITC.

---

### 3.3 GSTR-3B Non-Filer / Suspended Supplier ITC Risk
**Legal Basis**: Section 16(2)(c); Rule 86A; Circular 195/07/2023
**PRD Ref**: `resources/RULE ENGINE/GSTR 2A/3B Non Filer/` (PRD)
**Business Value**: 🔴 CRITICAL — ITC from non-filer/cancelled suppliers is blocked by portal automatically

**Risk Tiers**:

| Supplier Status | Risk Level | Recommended Action |
|---|---|---|
| 1–2 months non-filing | 🟡 MEDIUM | Send reminder to supplier |
| 3+ months non-filing | 🔴 HIGH | Reverse ITC proactively, pay interest |
| GSTIN Suspended | 🔴 HIGH | Hold all pending ITC claims from this vendor |
| GSTIN Cancelled | 🔴 CRITICAL | ITC permanently ineligible — raise debit note to supplier |

---

### 3.4 Rule 86B Electronic Credit Ledger Restriction
**Legal Basis**: Rule 86B, CGST Rules 2017; Notification 94/2020-CT
**PRD Ref**: `resources/RULE ENGINE/86 B/` (PRD)
**Business Value**: 🟡 HIGH — applies to large taxpayers

**Rule Logic**:
- Applies when: monthly taxable output > Rs 50 lakhs
- Restriction: minimum **1% of output tax liability must be paid in cash** (max 99% from ITC)
- Exemptions: directors who have filed IT returns, refund claimants, government entities, exporters (with refunds > Rs 1 lakh in prior FY)
- Check: GSTR-3B cash tax payment >= 1% of output tax liability for each period
- Flag: if cash paid < 1% → compute shortfall + notice risk level

---

### 3.5 Place of Supply (POS) Validation
**Legal Basis**: Sections 10–13, IGST Act 2017
**PRD Ref**: `resources/RULE ENGINE/GSTR 2A/POS/` + `resources/RULE ENGINE/GSTR1 POS/` (PRDs)
**Business Value**: 🟡 HIGH — wrong POS = wrong tax head (IGST vs CGST+SGST) = demand + interest

**Rule Logic**:
- **Goods** (Section 10): POS = location where movement terminates (destination state)
- **Services** (Section 12): POS = location of recipient (generally)
- **Bill-to Ship-to** (Section 10(1)(b)): POS = ship-to state, but deemed supply provisions apply
- **Import of services** (Section 13): POS = location of importer
- Compare: supplier's declared POS in GSTR-1/2A vs correct POS derived from transaction data
- Flag: IGST charged on intrastate supply (or CGST+SGST on interstate) → demand for differential + interest

---

## 🚀 Phase 4: Export, Concessional Rate & Advanced Compliance (Q1 2027)
**Goal**: Cover complete GST compliance lifecycle. 4 modules.

### 4.1 Export Flagging & LUT Compliance
**Legal Basis**: Section 16(3) IGST Act; Rule 89/96 CGST Rules; Notification 37/2017-CT
**PRD Ref**: `resources/RULE ENGINE/Export flagging/` (PRD)
**Business Value**: 🟡 HIGH — export refunds are high-value and heavily audit-intensive

**Rule Logic**:
- Identify GSTR-1 records in Table 6A (Exports)
- Validate: **With payment of IGST** (refund under Rule 96) OR **Under LUT/Bond** (zero-rated without payment)
- Flag: IGST paid on export but refund not claimed within **2-year limitation** (Section 54(1))
- Flag: LUT expired/not filed but export claimed as LUT-based → IGST liability undischarged
- Cross-check: Shipping bill date vs. GSTR-1 export date (must be within 3 months)
- Check: ITC accumulation on inputs used in zero-rated supply — refund eligibility under Rule 89(4)/(4A)

---

### 4.2 Concessional Rate Eligibility Validator (0.1% Supply)
**Legal Basis**: Notification 02/2019-CT (Rate); Section 9 CGST Act
**PRD Ref**: `resources/RULE ENGINE/0.1 supply in GSTR-1/` + `resources/RULE ENGINE/GSTR 2A/Concessional rate check/` (PRDs)
**Business Value**: 🟡 HIGH — e-commerce operators and special category supplies

**Rule Logic**:
- Verify conditions for concessional 0.1% rate: intra-state, registered buyer, specified goods category
- Cross-check: recipient GSTIN validity and registration status at time of supply
- HSN-rate validation: verify rate applied matches applicable rate per notification for that HSN in that FY
- Flag: concessional rate claimed but conditions not met → taxable at standard rate + demand for differential

---

### 4.3 Rule 42/43 — ITC Reversal for Exempt + Taxable Mixed Supplies
**Legal Basis**: Rule 42 (inputs/input services) & Rule 43 (capital goods), CGST Rules 2017
**Business Value**: 🟡 HIGH — one of the most complex calculations in GST; commonly done incorrectly

**Rule Logic**:
- Applicable when: taxpayer makes both taxable AND exempt supplies
- **Rule 42**: Monthly proportionate reversal of ITC on inputs/input services used for exempt + taxable
  - Formula: `ITC to reverse = Total ITC × (Exempt turnover / Total turnover)`.
  - **Securities/Shares Rule**: Include 1% of sale value of securities/shares in "Exempt turnover" as per Explanation 2 to Rule 42(1).
  - Exclude: zero-rated supply from exempt turnover for this computation.
  - Annual reconciliation: true-up in GSTR-3B of September (or within 6 months of FY end).
- **Rule 43**: Capital goods — same logic but over **5-year useful life** (60 months)
- Flag: reversal not done or incorrectly computed → demand exposure.
- Output: period-wise reversal schedule with IGST/CGST/SGST split.

---

### 4.4 Job Work (ITC-04) Compliance
**Legal Basis**: Section 143, CGST Act 2017; Rule 45, CGST Rules.
**Business Value**: 🟡 HIGH — Manufacturing CAs struggle with tracking goods at job-work locations.

**Rule Logic**:
- Track period: Goods must return within 1 year (Inputs) or 3 years (Capital Goods).
- **Rule 45(3)**: Half-yearly return (turnover > 5Cr) or Annual return (turnover <= 5Cr).
- Flag: if goods are not returned within the time limit (deemed supply per Section 143(3)/(4)).
- Check: Interest liability (18%) from the date goods were sent if deemed supply occurred.
- Reconcile: E-way bills (Outward to Job worker) vs Returns (Inward from Job worker).

---

## 🤖 Phase 5: AI-Powered Audit Simulation & Notice Defence (Q2 2027)
**Goal**: Premium tier — make GSTbuddies a CA's defence shield. 3 modules.

### 5.1 ASMT-10 / DRC-01 / DRC-01B Notice Reply Generator
**Legal Basis**: Sections 61, 73, 74 CGST Act; ASMT-10, DRC-01, DRC-01A, DRC-01B forms
**Business Value**: 🔴 CRITICAL — every CA handling GST notices needs this

- Input: all audit findings from Phases 1–4 + taxpayer's supporting documents
- Auto-generate structured reply: **Facts → Law → Computation → Prayer**
- Include: exact legal citations, CBIC circular references, and step-by-step computation
- Export: formatted PDF notice reply ready for CA review and signature
- DRC-01B: auto-generated demand for GSTR-1 vs GSTR-3B mismatch — pre-draft response

---

### 5.2 Annual Compliance Score & Risk Dashboard
**Business Value**: 🟡 HIGH — core premium subscription feature

- Aggregate all audit findings across FY into a **Compliance Score (0–100)**
- Risk heat-map: by return type, tax period, and tax head
- Trend: month-over-month improvement/deterioration
- Benchmarking: "Your ITC match rate is 91.2% vs industry average 94.6%"
- Multi-client portfolio view for CA firms (manage 50+ GSTINs from one dashboard)

---

### 5.3 GSTR-9 vs GSTR-3B Annual Delta Report
**Legal Basis**: GSTR-9 Tables 6, 7, 8; GSTR-9C Part III/IV/V
**Business Value**: 🟡 HIGH — annual return preparation tool

- Auto-aggregate 12-month GSTR-3B data vs GSTR-9 draft
- Identify delta items: outward supply differences, ITC differences, tax payment differences
- Map to GSTR-9C reconciliation tables (Part III: Turnover, Part IV: Tax Paid, Part V: ITC)
- Flag: items requiring additional tax payment or ITC reversal in annual return
- Generate: GSTR-9C reconciliation statement draft for CA review

---

## ⚙️ Engineering Architecture — Rule Engine Framework (Java 21 / Spring Boot 3.5)

Before Phase 1, extract a shared engine from Rule 37:

```java
// All modules implement this contract
public interface AuditRule<I, O extends AuditFinding> {
    String getRuleId();           // e.g., "RULE_LATE_FEE_GSTR1"
    String getLegalBasis();       // e.g., "Section 47(1), CGST Act 2017"
    O execute(I input, TenantContext ctx);
    int getCreditsRequired();
}

// Unified output format
public record AuditFinding(
    String ruleId,
    Severity severity,           // CRITICAL | HIGH | MEDIUM | LOW | INFO
    String legalBasis,
    String compliancePeriod,     // "FY: 2024-25, Tax Period: Apr-2024"
    BigDecimal impactAmount,
    String description,
    String recommendedAction,
    boolean autoFixAvailable
) {}
```

**New DB tables** (Flyway migration):
```sql
CREATE TABLE audit_runs (
  id UUID PRIMARY KEY, tenant_id UUID NOT NULL, user_id UUID NOT NULL,
  rules_executed TEXT[], total_impact_amount DECIMAL(18,2),
  status VARCHAR(20), credits_consumed INT,
  created_at TIMESTAMPTZ DEFAULT now(), completed_at TIMESTAMPTZ
);

CREATE TABLE audit_findings (
  id UUID PRIMARY KEY, run_id UUID REFERENCES audit_runs(id),
  tenant_id UUID NOT NULL, rule_id VARCHAR(100), severity VARCHAR(20),
  legal_basis TEXT, compliance_period VARCHAR(20),
  impact_amount DECIMAL(18,2), description TEXT,
  recommended_action TEXT, auto_fix_available BOOLEAN,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE late_fee_relief_windows (
  id SERIAL PRIMARY KEY, return_type VARCHAR(10),
  notification_no VARCHAR(100), start_date DATE, end_date DATE,
  fee_cgst_per_day DECIMAL(8,2), fee_sgst_per_day DECIMAL(8,2),
  max_cap_cgst DECIMAL(12,2), max_cap_sgst DECIMAL(12,2),
  applies_to VARCHAR(20), notes TEXT
);

-- Parser Pipeline (D6/D7)
CREATE TABLE parser_templates (
  id SERIAL PRIMARY KEY,
  template_id VARCHAR(100) UNIQUE NOT NULL,   -- e.g., 'CBIC_NOTIFICATION_V2'
  doc_type VARCHAR(20) NOT NULL,              -- 'PDF' | 'EXCEL'
  fingerprint JSONB NOT NULL,                 -- {"must_contain": [...], "min_pages": 1}
  extraction_rules JSONB NOT NULL,            -- anchors, regex, column aliases, table configs
  version INT DEFAULT 1,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE parsed_documents (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL,
  original_filename VARCHAR(500) NOT NULL,
  doc_type VARCHAR(20) NOT NULL,              -- 'PDF' | 'EXCEL'
  s3_raw_key VARCHAR(500) NOT NULL,           -- s3://gst-buddy-docs/raw/{tenant}/{file}
  template_id VARCHAR(100),                   -- NULL if no template matched (exception queue)
  parsed_json JSONB,                          -- normalised domain model output
  parser_version VARCHAR(20) NOT NULL,        -- e.g., '1.0.3'
  parse_status VARCHAR(20) NOT NULL,          -- 'SUCCESS' | 'PARTIAL' | 'FAILED' | 'PENDING_REVIEW'
  parse_duration_ms INT,
  error_message TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_parsed_docs_tenant ON parsed_documents(tenant_id);
CREATE INDEX idx_parsed_docs_status ON parsed_documents(parse_status);
```

---

## 📅 Delivery Timeline Summary

| Phase | Modules | Timeline | Credits/Run | Strategic Impact |
|---|---|---|---|---|
| Phase 0 | Rule 37 (MVP) + Platform (payments, RBAC, referrals) | ✅ Deployed | 1 | Foundation |
| Phase 1 | Late Fees (GSTR-1, 3B) + Interest + GSTR-9/9C | Q2 2026 | 1 each | Monthly CA retention |
| **Infra** | **Python Parser Sidecar (D6) + Template Registry + S3 storage (D7)** | **Q2–Q3 2026** | — | **Cross-cutting prerequisite for Phase 2+** |
| Phase 2 | Cross-return recon + ITC recon + RCM + Late invoices | Q3 2026 | 2–3 each | Audit trigger prevention |
| Phase 3 | Supplier risk + 16(4) deadline + Non-filer + 86B + POS | Q4 2026 | 2–3 each | ITC protection shield |
| Phase 4 | Exports/LUT + Concessional rate + Rule 42/43 + Job Work | Q1 2027 | 2–3 each | Full lifecycle |
| Phase 5 | AI notice drafting + Compliance score + GSTR-9 delta | Q2 2027 | 5–10 | Premium tier |

---

## 💰 Monetisation Strategy — Credit-Based Model

All audit runs consume credits. No unlimited subscriptions — every run costs credits, driving **predictable recurring revenue** as CAs process clients monthly.

### Credit Pricing per Module

| Module Category | Credits/Run | Rationale |
|---|---|---|
| **Phase 0**: Rule 37 (ITC 180-day reversal) | 1 credit | MVP baseline |
| **Phase 1**: Late Fee Calculators (GSTR-1, 3B, 9/9C) | 1 credit each | High frequency, low complexity — 1 credit keeps adoption high |
| **Phase 1**: Section 50 Interest Engine | 1 credit | Paired with late fee runs |
| **Phase 2**: GSTR-1 vs 3B vs 9 Reconciliation | 3 credits | Multi-return processing, high compute |
| **Phase 2**: ITC Reconciliation (2B vs Books) | 3 credits | Bulk invoice matching, highest value output |
| **Phase 2**: RCM Reconciliation | 2 credits | Medium complexity |
| **Phase 2**: Late Invoice Reporting Check | 1 credit | Single-return analysis |
| **Phase 3**: Supplier Risk Scoring | 2 credits | GSTIN API lookups + scoring |
| **Phase 3**: Section 16(4) Deadline Guard | 1 credit | Date-based check |
| **Phase 3**: Non-Filer Supplier Risk | 2 credits | 2A/2B cross-referencing |
| **Phase 3**: Rule 86B Restriction Check | 1 credit | Simple threshold check |
| **Phase 3**: POS Validation | 2 credits | Invoice-level state verification |
| **Phase 4**: Export & LUT Compliance | 2 credits | Cross-document matching |
| **Phase 4**: Concessional Rate Validator | 1 credit | HSN-rate lookup |
| **Phase 4**: Rule 42/43 ITC Reversal | 3 credits | Most complex computation |
| **Phase 4**: Job Work (ITC-04) Compliance | 2 credits | Time-limit tracking + deemed supply |
| **Phase 5**: Notice Reply Generator | 5 credits | AI-powered, high-value output |
| **Phase 5**: Annual Compliance Score | 3 credits | Full-FY aggregation |
| **Phase 5**: GSTR-9 Delta Report | 3 credits | 12-month reconciliation |

### Current Plans (MVP — Live)

| Plan | Credits | Price (INR) | Trial? | Notes |
|---|---|---|---|---|
| Trial | 2 | ₹0 | Yes | Auto-granted on first login (signup + SSO) |
| Pro | 5 | ₹1,000 | No | Early adopter pack |
| Ultra | 30 | ₹3,000 | No | Power user pack |

### Target Credit Packs (Phase 2+ — Planned)

Once multi-rule audit runs are available, transition to volume-based CA packs:

| Pack | Credits | Price | Per-Credit Cost | Target User |
|---|---|---|---|---|
| Starter | 10 credits | Rs 199 | Rs 19.90 | Individual taxpayer, try-before-buy |
| CA Basic | 50 credits | Rs 799 | Rs 15.98 | Small CA (5–10 clients) |
| CA Pro | 150 credits | Rs 1,999 | Rs 13.33 | Mid-size CA firm (20–30 clients) |
| CA Enterprise | 500 credits | Rs 4,999 | Rs 10.00 | Large firm (50+ clients) |
| Bulk | 1,500 credits | Rs 9,999 | Rs 6.67 | Enterprise / white-label partners |

**Free tier**: 2 credits auto-granted on first login (covers both signup pipeline AND SSO/social login via Grant-on-Login pattern). No monthly free credits — drives conversion.

### Revenue Math (Conservative — Based on Target Pricing)

> **Note**: Projections below use target CA-tier pricing (Phase 2+), not current MVP plans.

A CA with 25 clients running Phase 1 (4 modules × 1 credit × 12 months) + Phase 2 (3 modules × 2.5 avg credits × 4 quarters) = **~150 credits/quarter = Rs 1,999/quarter = Rs 8,000/year per CA**.
At 500 active CAs = **Rs 40 lakhs/year ARR** from credits alone.

---

## 📋 Immediate Next Steps (Phase 1 Kick-off)
- [ ] Extract `AuditRule` interface + `AuditRuleRegistry` from Rule 37 codebase
- [ ] Create Flyway migration for `audit_runs`, `audit_findings`, `late_fee_relief_windows`
- [ ] Implement GSTR-1 Late Fee Engine (PRD: `resources/RULE ENGINE/LATE FEES GSTR-1/`)
- [ ] Implement GSTR-3B Late Fee Engine (PRD: `resources/RULE ENGINE/LATE FEES GSTR-3B/`)
- [ ] Implement Section 50 Interest Engine (PRD: `resources/RULE ENGINE/Interest late filing GSTR3B/`)
- [ ] Implement GSTR-9/9C Late Fee Calculator (PRD: `resources/RULE ENGINE/GSTR 9 and 9C Late Fess/`)
- [ ] Upgrade Frontend Audit Dashboard for multi-rule result display

---

## ✅ Design Decisions (Finalized)

### D1. Data Input — Dual Mode (Manual + API)
**Decision**: Support **both** manual Excel/JSON upload AND GSP/ASP API auto-pull.
- Phase 1–2: Manual upload only (Excel/JSON from GST portal downloads).
- Phase 3+: Add optional GSP integration for auto-fetch (requires GSP empanelment or partnership with an existing GSP like ClearTax/Masters India). Auto-pull from GST portal requires **taxpayer consent via OTP** — this is legally permitted under the GSP framework but needs a registered ASP/GSP license. See `docs/ASP.md` for integration planning.
- **Rationale**: Manual-first reduces time-to-market. API is a Phase 3+ premium feature.

### D2. Credit Model — 1 Credit per Simple Rule
**Decision**: **1 credit** for all Phase 1 modules (late fees, interest). Same as Rule 37.
- Simple rules (single-return, single-computation) = 1 credit.
- Complex rules (multi-return reconciliation, bulk invoice matching) = 2–3 credits.
- Premium AI features (notice drafting) = 5 credits.
- **Rationale**: Low barrier keeps CAs running Phase 1 frequently (monthly habit). Revenue scales through volume, not per-unit price.

### D3. Multi-Return Upload — Piecemeal with Smart Prompting
**Decision**: Allow **piecemeal upload** — each return can be uploaded independently.
- GSTR-1 analysis works standalone (late fee, POS check).
- GSTR-3B analysis works standalone (interest, late fee, RCM).
- Cross-return reconciliation (Module 2.1: 1 vs 3B vs 9) **prompts** user to upload missing returns but runs partial analysis on what's available.
- **GST Expert Rationale**: In practice, CAs rarely have all returns ready simultaneously. A CA might download GSTR-1 today and 3B next week. Forcing simultaneous upload kills adoption. The system should be smart enough to run what it can and flag "upload GSTR-3B to complete liability reconciliation".

### D4. User Access — Open to All (Not CA-Only)
**Decision**: Show findings to **all users directly** — CAs, non-CA auditors, GST officers, and taxpayers.
- No white-label restriction on findings display.
- CA firms get **multi-client portfolio view** as a premium feature (not a gating mechanism).
- **Rationale**: Market is broader than CAs alone. Internal auditors, CFOs, and even GST department officers could use this for verification. Open access = larger TAM.

### D5. Rule 42/43 — Fully Automated with CA Override
**Decision**: **Compute automatically** with a "CA Override" toggle.
- System auto-computes monthly proportionate reversal (Rule 42) and capital goods reversal (Rule 43) based on uploaded GSTR-3B data.
- Annual true-up (September GSTR-3B) is auto-calculated when full-year data is available.
- CA can **override** individual values (exempt turnover, non-business use %) before finalizing.
- **GST Expert Rationale**: Rule 42/43 has clear formulas defined in law. The computation itself is deterministic — the subjectivity lies in classifying exempt vs. non-exempt turnover. Auto-compute with override gives CAs speed without sacrificing control. Flagging-only would be a missed opportunity since the math IS the value.

### D6. Data Ingestion — Deterministic Extraction Pipeline (Python Sidecar)
**Decision**: Use a Python-based HTTP sidecar (FastAPI) for robust, AI-free PDF/Excel parsing.

**Deployment Topology (Option 2 — HTTP Sidecar on Same EC2)**:
```
┌─────────────── Single EC2 Instance ───────────────┐
│                                                     │
│   Java (Spring Boot)          Python (FastAPI)       │
│   ┌──────────────┐           ┌──────────────┐       │
│   │  Main API    │──HTTP────▶│  Parser API  │       │
│   │  Port 8080   │◀──JSON────│  Port 8090   │       │
│   └──────────────┘           └──────────────┘       │
│                                                     │
│   systemd: gst-api            systemd: gst-parser    │
└─────────────────────────────────────────────────────┘
```
- Two independent `systemd` services on the same EC2. If the parser crashes on a malformed PDF, `systemd` auto-restarts it; the Java JVM is unaffected.
- Java calls `POST http://localhost:8090/api/v1/extract` via `RestClient` with configurable timeout (default: 30s).
- **Future scaling**: Change `parser.service.url` from `localhost:8090` to an internal ALB/ECS endpoint — zero Java code change.

**Pipeline Architecture (4-Stage Deterministic Router)**:
```
File In → [1. Classify] → [2. Route] → [3. Extract] → [4. Validate] → JSON Out
                                              │
                                     ┌────────┴─────────┐
                                     │ Exception Queue   │
                                     │ (Human review UI) │
                                     └──────────────────┘
```
1. **Classifier**: Reads first page text + MIME type. Matches against `parser_templates.fingerprint` (keyword sets like `"Government of Maharashtra"`, `"Notification No."`). Fast keyword matching — no ML.
2. **Router**: If fingerprint matches a known `template_id` → route to deterministic extractor. If no match → route to Exception Queue.
3. **Extractors** (format-specific, config-driven):
   - **PDF — Spatial Anchoring**: Find anchor text (e.g., "Notification No.") by coordinates using `pdfplumber`, then extract data from a geometric bounding box relative to the anchor. Font-size heuristics detect section headers (bold / >14pt) vs. body text.
   - **PDF — Table Extraction**: `camelot` (lattice mode) detects physical gridlines using CV; `pdfplumber` for borderless tables using text alignment.
   - **Excel — Alias Mapping Engine**: Maintains a dictionary of column header aliases (e.g., `"Taxable Value"` → `["taxable val", "txbl val", "tax value", "base amount"]`). Scans first 50 rows for the row where ≥3 aliases match → that's the header row. Uses `pandas.read_excel(skiprows=N)` + `pd.to_numeric(errors='coerce')` for type coercion.
4. **Validator**: Checks extracted output against domain rules (e.g., GST rate ∈ {0, 0.1, 5, 12, 18, 28}%, GSTIN checksum valid, notification number format matches `NN/YYYY-CT`). If validation fails → Exception Queue.

**Template Registry** (`parser_templates` DB table — not hardcoded):
- Each government document layout is described by a JSON configuration (fingerprint + extraction rules).
- When a new format appears, an engineer adds a ~10-line JSON config — no Python code changes needed.
- Templates are versioned (`version` column) for retroactive re-parsing.

**LLM Policy**: LLMs are **not used** in the default pipeline. Documents that fail classification are routed to an Exception Queue for human review. Once a human identifies the layout, a new template config is added, and the file is re-processed deterministically. LLM-based extraction may be added as an optional premium fallback in Phase 5.

**Key Python Libraries**:
| Library | Purpose | GST Use Case |
|---|---|---|
| `PyMuPDF (fitz)` | Fastest text + coordinate extraction (C-backed) | Base PDF text layer — 3–5x faster than Java PDFBox |
| `pdfplumber` | Table detection + visual debugging | Rate schedules, HSN tables, relief window tables |
| `camelot` | ML-based table boundary detection (lattice/stream) | Complex multi-column tables in CBIC circulars |
| `pandas` + `openpyxl` | Excel parsing with dynamic header detection | GSTR-2B offline utility, purchase registers, HSN master |

**Rationale**: Python's PDF/Excel ecosystem is 3–5x faster and has native table detection that Java lacks entirely. HTTP sidecar gives crash isolation, independent restarts, and a clean migration path to ECS/Fargate.

---

### D7. Raw Document Storage — WORM (Write-Once, Read-Many) Pattern
**Decision**: Store all raw government uploaded PDFs/Excels persistently alongside their parsed JSON representations.

**Storage Layout**:
```
S3:   s3://gst-buddy-docs/raw/{tenant_id}/{doc_type}/{notification_no}_{date}.{ext}
PSQL: parsed_documents table → parsed_json (JSONB) + s3_raw_key (reference)
```
- **Raw files** in AWS S3 Standard-IA (cost: ~₹2/month for thousands of files).
- **Parsed output** in PostgreSQL `JSONB` (queryable, indexable).
- **Never store raw binary blobs in PSQL** — it bloats DB backups.
- `parser_version` column enables retroactive re-parsing: when a template is upgraded, re-run extraction on the original S3 file and update `parsed_json`.

**Rationale**:
- **Legal**: GST audits (Section 65/66) and demand proceedings (Section 73/74) may require proof of which version of a notification was relied upon. The raw file is evidence.
- **Re-parsing**: Government PDF formats drift. When the parser template is improved, re-process the original file without re-downloading from an unreliable government portal.
- **Portal Decoupling**: CBIC/GST portal has frequent downtime. Once ingested, the system is fully independent of portal availability.

---

_Disclaimer: This roadmap is subject to changes based on GST Council recommendations, CBIC notifications, and Union Budget amendments._
_Next review: End of Q2 2026 (after Phase 1 delivery)_
