# GSTbuddies Issues Tracker

Last updated: 2026-03-18

## Purpose

This document tracks review findings, release blockers, improvements, and product suggestions for GSTbuddies.

Primary focus for this review:

- GST legal correctness of values shown in the UI
- Rule 37 engine correctness
- Export correctness
- Production release readiness

## Release Recommendation

Current recommendation: `Proceed to Beta / QA Release`

Reason:

- All identified GST legal correctness and Rule 37 engine blockers (P0/P1) are resolved.
- UI contract mismatches for fresh uploads are fixed.
- UI labels now accurately reflect the "estimate" nature of GST results where applicable.
- GSTR-3B summary and Detailed Excel exports are now legally and procedurally complete.
- **Pending**: Frontend test coverage (GST-008) and deferred landing page content (GST-011).

## Legal Reference Points Used In This Review

These references were used to tighten the review. They are official or official-support sources and should guide implementation wording:

- Section 16(2) second proviso of the CGST Act: recipient must pay supplier value plus tax within 180 days, else reversal condition applies.
- Notification No. 19/2022-Central Tax dated 28.09.2022:
  - Rule 37 was updated with effect from 01.10.2022.
  - Rule 37 now explicitly excludes supplies on which tax is payable on reverse charge basis.
  - It also deems certain values as paid for Schedule I supplies and section 15(2)(b) additions.
  - It links payment to FORM GSTR-3B for the tax period immediately following the 180-day period.
- Notification No. 14/2022-Central Tax dated 05.07.2022:
  - Introduced rule 88B on manner of calculating interest on delayed payment of tax.
- Circular No. 170/02/2022-GST dated 06.07.2022:
  - Clarifies reclaimable reversals such as Rule 37 are reported in Table 4(B)(2) and reclaimed in Table 4(A)(5), with Table 4(D)(1) disclosure.
- GST portal tutorial for GSTR-3B:
  - Reclaimable reversals may be reported in 4(B)(2) and reclaimed in 4(A)(5).

## Status Legend

- `open` = identified, not yet fixed
- `in_progress` = fix work started
- `blocked` = waiting on product/legal/data decision
- `done` = fixed and verified
- `deferred` = intentionally postponed

## Priority Legend

- `P0` = release blocker
- `P1` = must fix before production if GST-facing
- `P2` = important improvement
- `P3` = nice to have

## Review Tracker

| ID | Priority | Status | Area | Type | Short Title |
|---|---|---|---|---|---|
| GST-001 | P0 | done | Backend Parser | Bug | Opening balance rows can be treated as taxable purchases |
| GST-002 | P0 | done | Rule 37 Engine | Legal correctness | ITC amount hardcoded to `18/118` |
| GST-003 | P0 | done | Rule 37 Engine / UI | Legal correctness | Interest and re-availment timeline is not legally reliable |
| GST-004 | P0 | done | Backend DTO / Frontend UI | Bug | Upload response shape does not match GST UI contract |
| GST-005 | P1 | done | Export / Frontend | Bug | `GSTR-3B Summary` export button downloads wrong report |
| GST-006 | P1 | done | Frontend UI | UX / Compliance | Dashboard can show `All Clear` even when invoices are at risk |
| GST-007 | P1 | done | UI Copy / Marketing | Legal wording | Product copy overstates certainty of GST outcomes |
| GST-008 | P2 | open | Frontend Testing | Test gap | No GST-critical frontend tests for upload/result rendering |
| GST-009 | P2 | done | Product Scope | Product clarity | Scope assumptions are not explicit to users |
| GST-010 | P0 | done | Rule 37 Scope | Legal correctness | Engine cannot identify supplies excluded from Rule 37 |
| GST-011 | P0 | deferred | Landing Page | Legal / Credibility | Landing page contains unverifiable marketing claims (On hold) |
| GST-012 | P1 | done | UI Labels | GST terminology | "Invoice Value" column should read "Ledger Amount (Tax-Inclusive)" |
| GST-013 | P1 | done | UI Labels | GST terminology | "Est. ITC" column label is ambiguous — should clarify it is derived at 18% |
| GST-014 | P1 | done | UI Labels | GST terminology | Show potential ITC/Reversal for AT_RISK rows to provide early warning |
| GST-015 | P1 | done | Export / Backend | GST correctness | GSTR-3B Summary export misses interest column (Table 4(B)(2) is reversal only, but interest should be shown separately) |
| GST-016 | P1 | done | Export / Backend | Data completeness | Excel export has no Invoice Number column |
| GST-017 | P2 | done | Landing Page | Typo | Rule 37 explainer section has typo: "afetr" should be "after" |
| GST-018 | P1 | done | UI Labels | Professional quality | Column "How Late" is informal — should be "Delay (Days)" |
| GST-019 | P2 | done | UI Labels / Legal | GST terminology | "File In" column header is unclear — should be "GSTR-3B Return Period" |
| GST-020 | P1 | done | Calculation Engine | GST correctness | Interest charged on PAID_LATE invoices where payment is within 180 days but after ITC availment date |
| GST-021 | P1 | done | UI / Professional | UX quality | Remove redundant "Est." prefix on every column — use a single header-level disclaimer instead |

## Issue Validation Matrix

This section answers two practical questions:

- Is this a real issue or only a theoretical concern?
- If we fix it, how do we avoid regressions?

| ID | Validation Result | Confidence | Why I am confident | Safe-fix regression guard |
|---|---|---|---|---|
| GST-001 | `Confirmed current defect` | High | Existing test already proves opening-balance-like row is parsed as a live transaction | Add parser fixtures for opening balance, carried forward balance, totals, and non-invoice rows |
| GST-002 | `Confirmed release issue` | High | Engine has only one amount field and hardcoded `18/118`; no rate metadata exists anywhere in the flow | Add input-schema validation and rate-aware tests before changing formula behavior |
| GST-003 | `Confirmed release issue, but partly a certainty/labeling issue` | Medium-High | Engine invents availment/reclaim timeline from heuristics not present in source data | Split estimate vs exact output and test labels separately from computations |
| GST-004 | `Confirmed current defect` | Very High | Upload DTO omits fields that the result UI directly reads; this can already produce malformed rendering | Contract tests for upload payload and saved-run payload using the same component fixture |
| GST-005 | `Confirmed current defect` | Very High | Frontend code drops `gstr3b` and always calls issues/complete only | Unit test request params and download naming by report type |
| GST-006 | `Confirmed current defect in UI policy` | High | Banner logic ignores at-risk invoices entirely | Banner-state tests for clear / watchlist / action-required |
| GST-007 | `Confirmed release issue` | High | Product copy makes stronger claims than the present engine can support | Review all GST-facing copy against supported scope before release |
| GST-008 | `Confirmed gap` | Very High | No GST-critical frontend tests were found for result rendering or export selection | Add tests before or alongside fixes so future changes stay safe |
| GST-009 | `Confirmed release issue` | High | Supported input assumptions are not disclosed or enforced | Add upload validation and scope messaging tests |
| GST-010 | `Confirmed release issue` | High | Current domain model cannot mark reverse charge or other excluded Rule 37 cases | Add classification inputs or explicit upload declarations plus exclusion tests |

## What Is Definitely Broken Vs What Is Scope-Dependent

### Definitely broken in the current build

- GST-001
- GST-004
- GST-005
- GST-006

These are observable from the current code path and do not depend on legal interpretation or special business assumptions.

### Real issues because of current product positioning

- GST-002
- GST-003
- GST-007
- GST-009
- GST-010

These become critical because the product is being positioned for production GST use with legally correct values in the UI. If the product were openly labelled as a narrow-scope estimate tool and enforced that scope at upload, some of these would move from `P0/P1` legal issues to controlled product limitations. In the current state, they are release issues.

### Structural quality gaps

- GST-008

This is not a user-visible defect by itself, but without this layer of tests the GST-facing fixes will be harder to trust.

## Regression-Safe Fix Principles

We should fix these in a way that reduces risk, not increases it.

1. Add failing tests first for every confirmed defect.
2. Preserve current passing tests only where the current behavior is actually desired.
3. Replace legally wrong tests, do not keep them as legacy expectations.
4. Use real or anonymized Tally/Busy sample ledgers as golden fixtures.
5. Make upload response and saved-run response share one domain contract where possible.
6. Separate exact statutory outputs from estimate/support outputs in both DTOs and UI labels.
7. Keep unsupported cases out of the engine by validation, not by silent approximation.

## Detailed Findings

### GST-001

- Priority: `P0`
- Status: `open`
- Area: `backend-service`
- Type: `Bug`
- Title: `Opening balance rows can be treated as taxable purchases`

Problem:

- The parser skips `opening`, `closing`, and `total` rows only when those words appear in column A.
- In common Tally creditor exports, opening balance semantics may appear in particulars/description columns instead.
- Current tests explicitly confirm this incorrect behavior.

Why this matters:

- Non-invoice opening balances can become fake Rule 37 purchase entries.
- That can create false ITC reversal and false interest liability.

Evidence:

- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/domain/ledger/LedgerExcelParser.java`
- `/home/pawan/personal/GSTbuddies/backend-service/src/test/java/com/learning/backendservice/domain/ledger/LedgerExcelParserTest.java`

Suggested fix:

- Add explicit non-transaction row detection using particulars, voucher type, invoice number, and business rules.
- Reject or ignore opening balance rows before Rule 37 classification.
- Add parser tests for Tally opening balance, carried-forward balance, and creditor opening ledger rows.

Acceptance criteria:

- Opening balance rows do not appear in Rule 37 calculation results.
- Regression tests fail without the fix and pass with it.

Precise review:

- Senior GST view:
  - Rule 37 is invoice-payment compliance, not opening-balance compliance.
  - An opening balance is not automatically proof of ITC availed on a current taxable invoice that has crossed the 180-day condition.
  - Treating brought-forward balances as current purchase invoices is legally unsound.
- CA / firm impact:
  - This is the kind of defect that will immediately destroy trust with finance teams because the software appears to create liability out of legacy balances.
  - A CA reviewing the output will flag this as a data-hygiene failure before even testing other logic.
- Engineering view:
  - Current parser logic is too positional.
  - It needs row classification, not only column extraction.
- Release stance:
  - Hard blocker. This must be fixed before any production GST-facing release.
- Reality check:
  - This is a real defect today, not a hypothetical one.
- Regression guard:
  - Build parser fixtures for:
    - opening balance in particulars
    - opening balance in narration
    - totals/closing rows
    - valid purchase rows adjacent to summary rows
  - Verify only valid purchase/payment rows survive.

---

### GST-002

- Priority: `P0`
- Status: `open`
- Area: `backend-service`
- Type: `Legal correctness`
- Title: `ITC amount hardcoded to 18/118`

Problem:

- ITC is computed using a fixed formula: `principal × 18 / 118`.
- This only works for a narrow case where amount is tax-inclusive and GST rate is 18%.

Why this matters:

- Real purchase ledgers can contain 5%, 12%, 18%, 28%, cess, exempt, and mixed supplies.
- Current UI presents values as if they are generally correct.
- This is not safe for production GST filing support.

Evidence:

- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/domain/rule37/Rule37InterestCalculationService.java`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/dashboard/dashboard.component.html`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/rule37/compliance-view/compliance-view.component.html`

Suggested fix:

- Decide and document supported input model:
  - Option A: require taxable value + tax amount columns
  - Option B: require GST rate per invoice
  - Option C: restrict v1 to 18% tax-inclusive invoices and state that very clearly
- Update engine, DTOs, export, and UI wording to match the supported model.

Acceptance criteria:

- ITC calculation basis is explicit, auditable, and correct for supported inputs.
- Unsupported ledger shapes are rejected with a clear validation message.

Precise review:

- Senior GST view:
  - Rule 37 reversal is of the ITC actually availed on the supply.
  - The code is not calculating "ITC availed"; it is inferring tax from a single amount field with a fixed 18% inclusive assumption.
  - That may be directionally acceptable only in a very narrow use case, but not as a general GST result.
- CA / firm impact:
  - Firms will often upload ledgers containing mixed-rate vendors, reimbursements, freight, exempt supplies, or entries where the amount is taxable value rather than invoice value.
  - In those cases, the present engine can materially overstate or understate reversal.
- Engineering view:
  - The parser and model currently store only one amount.
  - A filing-grade engine needs either:
    - taxable value plus tax amount,
    - or GST rate and whether value is tax-inclusive,
    - or invoice-level tax breakup.
- Release stance:
  - Hard blocker unless the product is explicitly limited to a narrow supported format and that limit is enforced at upload.
- Reality check:
  - This is unquestionably a real release issue.
  - The only conditional part is whether you want to solve it by richer data or by strict scope limitation.
- Regression guard:
  - Add scenario tests for:
    - 5% inclusive
    - 12% inclusive
    - 18% inclusive
    - 28% inclusive
    - taxable-value-only input
    - tax-inclusive mixed invalid upload

---

### GST-003

- Priority: `P0`
- Status: `open`
- Area: `backend-service`, `frontend`
- Type: `Legal correctness`
- Title: `Interest and re-availment timeline is not legally reliable`

Problem:

- ITC availment is estimated as the 20th of the next month.
- Interest is currently computed to payment date or `asOnDate`.
- Reclaim messaging is tied to a generated `gstr3bPeriod` based on the 180-day deadline.

Why this matters:

- Rule 37 and section 16 mechanics are more specific than the current approximation.
- Current UI can be read as filing guidance, not just risk guidance.
- This can overstate or understate liability and misstate when ITC may be re-availed.

Evidence:

- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/domain/rule37/Rule37InterestCalculationService.java`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/rule37/compliance-view/compliance-view.component.ts`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/rule37/compliance-view/compliance-view.component.html`

Suggested fix:

- Split concepts clearly:
  - risk identification
  - provisional reversal estimate
  - statutory interest liability
  - re-availment eligibility
- If exact legal chronology cannot be derived from available data, downgrade the UI wording from certainty to estimate.
- Consider requiring these dates separately if you want filing-grade accuracy:
  - actual ITC availment period
  - actual reversal period
  - date output tax liability was paid

Acceptance criteria:

- UI no longer presents estimated dates as definitive filing instructions.
- Interest and reclaim logic is either legally supportable or explicitly marked as estimate.

Precise review:

- Senior GST view:
  - Current Rule 37 after Notification 19/2022 requires payment of an amount equal to the ITC availed along with interest in FORM GSTR-3B for the tax period immediately following the 180-day period.
  - Re-availment happens when payment is subsequently made to the supplier.
  - Your code uses a synthetic availment date, a synthetic filing period, and a stop-date that may not match the actual period when the amount is paid through return mechanics.
- Important nuance:
  - Rule 88B primarily governs manner of calculating interest on delayed payment of tax.
  - Once the product starts making exact interest claims, the legal chronology becomes very sensitive to actual ledger, return, and payment events.
- CA / firm impact:
  - A CA may use your output as a working paper.
  - If the UI says "re-claim in this GSTR-3B period" and that period is not actually correct for that taxpayer's factual pattern, the software has crossed from estimate into wrong compliance advice.
- Engineering view:
  - The system currently conflates:
    - risk detection,
    - reversal estimation,
    - filing-period suggestion,
    - and interest computation.
  - These should be separate fields with separate labels.
- Release stance:
  - Hard blocker for "legally correct values" positioning.
  - Could be downgraded only if the entire UI is repositioned as estimate/risk support, not filing instruction.
- Reality check:
  - I am not saying every number generated today is wrong.
  - I am saying the current system cannot support the certainty of the labels it uses.
- Regression guard:
  - Add tests that distinguish:
    - estimate labels
    - exact statutory labels
    - reclaim eligibility text
    - filing-period suggestion text

---

### GST-004

- Priority: `P0`
- Status: `open`
- Area: `backend-service`, `frontend`
- Type: `Bug`
- Title: `Upload response shape does not match GST UI contract`

Problem:

- The immediate upload response omits fields expected by the compliance view:
  - `invoiceNumber`
  - `originalInvoiceValue`
  - `paymentDeadline`
  - `riskCategory`
  - `gstr3bPeriod`
  - `daysToDeadline`
  - `itcAvailmentDate`
  - summary enhancements like `atRiskCount`, `atRiskAmount`, `breachedCount`, `calculationDate`
- Backend upload DTO also sends unpaid `paymentDate` as `"Unpaid"` instead of `null`.

Why this matters:

- Immediate post-upload results can render malformed or wrong values.
- Saved history view and upload view may show different behavior for the same calculation.
- This is a serious trust issue in a tax UI.

Evidence:

- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/dto/UploadResult.java`
- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/service/LedgerUploadOrchestrator.java`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/shared/models/rule37.model.ts`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/dashboard/dashboard.component.ts`

Suggested fix:

- Align upload DTO and persisted run DTO to the same shape where possible.
- Use `null` for absent dates, never `"Unpaid"` as a date field.
- Add frontend contract tests for both upload and history render paths.

Acceptance criteria:

- Upload results and history results render identically for the same run.
- No `Invalid Date`, `NaN`, or undefined-risk UI states appear.

Precise review:

- Senior GST view:
  - In tax software, the first screen after upload is the most dangerous place to show wrong values because users treat it as the authoritative answer.
- CA / firm impact:
  - A history screen and a fresh-upload screen showing different behavior for the same run is unacceptable in an audit-support context.
- Engineering view:
  - This is a contract design defect, not only a UI bug.
  - The upload path and persisted-run path are serving different domain shapes to the same component.
  - That guarantees drift and hidden defects.
- Release stance:
  - Hard blocker. This is not cosmetic.
- Reality check:
  - This is a definite bug in the current implementation.
- Regression guard:
  - Snapshot or component tests for the same run rendered through:
    - fresh upload response
    - saved history response
  - Both should render identical visible values and statuses.

---

### GST-005

- Priority: `P1`
- Status: `open`
- Area: `frontend`, `backend-service`
- Type: `Bug`
- Title: `GSTR-3B Summary export button downloads wrong report`

Problem:

- UI emits `gstr3b`.
- Frontend export helpers coerce any non-`complete` value back to `issues`.
- Backend support exists, but frontend never actually requests `reportType=gstr3b`.

Why this matters:

- Users can believe they downloaded a GSTR-3B summary but actually receive a different report.

Evidence:

- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/rule37/compliance-view/compliance-view.component.ts`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/core/services/rule37-api.service.ts`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/dashboard/dashboard.component.ts`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/rule37/calculation-history/calculation-history.component.ts`
- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/service/export/Gstr3bSummaryExportStrategy.java`

Suggested fix:

- Extend frontend export typing to include `gstr3b`.
- Pass through the requested report type unchanged.
- Use server-provided filename or derive correct filename client-side by report type.

Acceptance criteria:

- Clicking `GSTR-3B Summary` downloads the GSTR-3B summary workbook.
- A frontend test verifies the request parameter.

Precise review:

- Senior GST view:
  - This is not a tax-law blocker by itself, but once a button is labelled as GSTR-3B, the user assumes the output maps to return preparation.
- CA / firm impact:
  - A CA downloading the wrong workbook under a GSTR-3B label is a process-control failure.
- Engineering view:
  - The backend strategy exists and is tested.
  - The defect is entirely in frontend parameter handling and file naming.
- Release stance:
  - Must fix before release because it creates direct user confusion in a compliance workflow.
- Reality check:
  - This is a definite bug today.
- Regression guard:
  - Add service tests asserting request param `reportType=gstr3b`.
  - Add UI tests asserting the clicked action maps to the correct API call.

---

### GST-006

- Priority: `P1`
- Status: `open`
- Area: `frontend`
- Type: `UX / Compliance`
- Title: `Dashboard can show All Clear even when invoices are at risk`

Problem:

- `All Clear` is driven only by total reversal and total interest.
- `AT_RISK` invoices between 150 and 180 days do not affect the banner.

Why this matters:

- Operationally, users may ignore time-sensitive suppliers because the top message implies no action or no concern.

Evidence:

- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/dashboard/dashboard.component.ts`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/dashboard/dashboard.component.html`

Suggested fix:

- Introduce three top-level states:
  - clear
  - watchlist / due soon
  - action required
- Show count and amount for at-risk invoices in the header and summary cards.

Acceptance criteria:

- If at-risk invoices exist, the banner does not say `All Clear`.

Precise review:

- Senior GST view:
  - A case can be legally non-breached today and still require urgent attention from finance.
  - If the product hides that urgency under `All Clear`, it weakens preventive compliance value.
- CA / firm impact:
  - Firms want early warning, not only post-default reporting.
  - This is especially relevant for monthly close and vendor follow-up.
- Engineering view:
  - The required data already exists in the domain model.
  - This is a presentation policy issue, not a backend limitation.
- Release stance:
  - Strong pre-release fix. It is not a statutory miscalculation, but it is a serious control-design issue.
- Reality check:
  - This is definitely happening with the current code.
- Regression guard:
  - Add deterministic banner tests for:
    - zero issues
    - only at-risk items
    - breached items

---

### GST-007

- Priority: `P1`
- Status: `open`
- Area: `frontend`, `marketing copy`
- Type: `Legal wording`
- Title: `Product copy overstates certainty of GST outcomes`

Problem:

- Current copy uses strong statements like:
  - interest from invoice date
  - exact liability style messaging
  - broad wording that implies filing-grade accuracy

Why this matters:

- Current implementation is still estimate-based in key places.
- Strong wording increases legal and customer support risk.

Evidence:

- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/landing/landing.component.html`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/dashboard/dashboard.component.html`
- `/home/pawan/personal/GSTbuddies/frontend/src/app/features/rule37/compliance-view/compliance-view.component.html`
- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/domain/rule37/CalculationSummary.java`

Suggested fix:

- Replace definitive language with calibrated wording until logic is filing-grade.
- Keep disclaimer, but do not rely on disclaimer to offset misleading primary copy.

Acceptance criteria:

- UI and landing page claims match actual supported scope and data quality.

Precise review:

- Senior GST view:
  - Disclaimers do not cure misleading primary messaging.
  - If the headline says one thing and the logic only estimates it, the product wording is still risky.
- CA / firm impact:
  - Copy like "interest from invoice date" or "exact liability" invites users to rely on the output beyond what the engine supports.
  - That can create engagement and liability issues for firms using the tool internally.
- Engineering view:
  - Product copy should follow actual supported domain assumptions, not aspirational behavior.
- Release stance:
  - Must fix before public positioning as a production compliance tool.
- Reality check:
  - This is a real release issue even if no code changes are needed.
- Regression guard:
  - Maintain a reviewed list of GST-facing phrases that require legal/product sign-off before release.

---

### GST-008

- Priority: `P2`
- Status: `open`
- Area: `frontend`
- Type: `Test gap`
- Title: `No GST-critical frontend tests for upload/result rendering`

Problem:

- Backend tests are present for parser and calculator behavior.
- Frontend GST views appear to have no dedicated tests covering:
  - upload response rendering
  - history response rendering
  - status banners
  - export flow selection

Why this matters:

- Legal-value bugs can survive even if backend tests are green.

Suggested fix:

- Add component/service tests for:
  - upload result contract
  - saved history contract
  - GSTR-3B export selection
  - at-risk vs all-clear banner states

Acceptance criteria:

- Critical GST UI states are covered by automated tests.

Precise review:

- Senior GST view:
  - In a compliance product, visual correctness is part of tax correctness.
  - If the wrong label or wrong date is displayed, the tax output is effectively wrong for the user.
- Engineering view:
  - Backend-only confidence is insufficient here because the GST risk is at the presentation layer as well.
- Release stance:
  - Important hardening item. Not the first thing to fix, but should be in the same release train.
- Reality check:
  - This is not speculative; the current frontend path has no meaningful GST regression net.
- Regression guard:
  - Add the tests before or together with fixes, not after.

---

### GST-009

- Priority: `P2`
- Status: `open`
- Area: `product`, `ux`, `legal`
- Type: `Product clarity`
- Title: `Scope assumptions are not explicit to users`

Problem:

- It is not clear to users whether the product assumes:
  - tax-inclusive amounts
  - single GST rate
  - no cess
  - clean purchase/payment ledgers only
  - exact filing-grade data versus risk-estimate mode

Why this matters:

- A compliance product must surface unsupported cases early, not after results are shown.

Suggested fix:

- Add a visible `Supported Inputs` section in upload flow.
- Reject unsupported files or show a high-visibility estimate warning.
- Publish a short methodology note for CA and finance teams.

Acceptance criteria:

- Users understand what the engine supports before uploading data.

Precise review:

- Senior GST view:
  - The safest GST products are explicit about unsupported cases.
  - Silence on scope is itself a compliance risk.
- CA / firm impact:
  - Firms can work with limited scope if the scope is declared honestly and enforced consistently.
  - Firms cannot work safely with hidden assumptions.
- Engineering view:
  - Scope clarity reduces both defect volume and support burden.
- Release stance:
  - Important pre-release product control. Even if some legal logic remains estimate-based, this clarity is mandatory.
- Reality check:
  - This is a real issue because unsupported assumptions are currently silent.
- Regression guard:
  - Add upload validation and a visible supported-input checklist to keep scope from drifting silently.

---

### GST-010

- Priority: `P0`
- Status: `open`
- Area: `product`, `backend-service`, `rule engine`
- Type: `Legal correctness`
- Title: `Engine cannot identify supplies excluded from Rule 37`

Problem:

- Current Rule 37 logic appears to apply to all parsed purchase entries.
- The engine has no explicit way to identify:
  - reverse charge supplies,
  - Schedule I supplies made without consideration,
  - section 15(2)(b) value additions deemed paid,
  - and other entries where Rule 37 mechanics should not be applied in the ordinary way.

Why this matters:

- Notification No. 19/2022-Central Tax explicitly excludes supplies on which tax is payable on reverse charge basis from rule 37(1).
- The same notification deems certain supplies/amounts as paid for purposes of the second proviso to section 16(2).
- If the engine cannot distinguish these cases, it can create false reversal recommendations.

Evidence:

- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/domain/rule37/Rule37InterestCalculationService.java`
- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/domain/ledger/LedgerEntry.java`
- `/home/pawan/personal/GSTbuddies/backend-service/src/main/java/com/learning/backendservice/domain/ledger/LedgerExcelParser.java`

Suggested fix:

- Extend input classification to capture supply nature or explicitly restrict supported uploads.
- At minimum, surface a mandatory declaration at upload:
  - whether ledger includes reverse charge entries,
  - whether amounts are tax-inclusive,
  - whether non-standard entries are present.
- Preferably, add invoice tags or filters for excluded categories.

Acceptance criteria:

- Rule 37 is not applied to entries excluded by law.
- Unsupported ledgers are rejected or clearly marked as out of scope.

Precise review:

- Senior GST view:
  - This is a major legal-scope issue, not a minor enhancement.
  - Rule 37 is not a blanket rule for every inward supply ledger line.
- CA / firm impact:
  - Reverse charge-heavy businesses will get distorted results if this is not addressed.
- Engineering view:
  - The present data model is too thin for this classification.
  - You either need richer inputs or explicit scope limitation.
- Release stance:
  - Hard blocker for a general-market production release.
- Reality check:
  - This is a real issue for any business uploading general purchase ledgers.
  - If you intentionally support only non-RCM standard creditor ledgers, that limitation must be enforced.
- Regression guard:
  - Add tests for excluded categories and verify the engine either:
    - rejects them, or
    - marks them unsupported, or
    - excludes them with explicit reasoning.

## Suggested Fix Order

### Phase 1: Production Blockers

- GST-001
- GST-002
- GST-003
- GST-004
- GST-010

### Phase 2: Release-Critical UX and Export

- GST-005
- GST-006
- GST-007

### Phase 3: Hardening

- GST-008
- GST-009

## Suggested Workstreams

### Legal correctness workstream

- Confirm Rule 37 assumptions with CA / GST specialist
- Define supported calculation scope for v1
- Align engine, exports, UI labels, and disclaimers to the same rule set

### Data contract workstream

- Unify upload DTO and saved run DTO
- Remove string placeholders from date fields
- Add typed frontend contract tests

### Parser hardening workstream

- Exclude opening balance and summary rows reliably
- Add test fixtures from real Tally/Busy exports

### UX hardening workstream

- Replace `All Clear` with a three-state compliance summary
- Make estimate vs exact values obvious
- Fix export naming and report selection

## Notes

- Targeted backend tests currently pass for parser, calculator, and export behavior.
- Passing tests do not currently prove legal correctness because several tests validate current assumptions rather than statutory correctness.

## Owners

Fill when assigned:

| ID | Owner | Target date | Notes |
|---|---|---|---|
| GST-001 |  |  |  |
| GST-002 |  |  |  |
| GST-003 |  |  |  |
| GST-004 |  |  |  |
| GST-005 |  |  |  |
| GST-006 |  |  |  |
| GST-007 |  |  |  |
| GST-008 |  |  |  |
| GST-009 |  |  |  |
| GST-010 |  |  |  |
| GST-011 |  |  |  |
| GST-012 |  |  |  |
| GST-013 |  |  |  |
| GST-014 |  |  |  |
| GST-015 |  |  |  |
| GST-016 |  |  |  |
| GST-017 |  |  |  |
| GST-018 |  |  |  |
| GST-019 |  |  |  |
| GST-020 |  |  |  |
| GST-021 |  |  |  |

---

## Final Pass Review — 2026-03-18 (Senior GST Officer)

Reviewer role: Senior GST Officer with 15+ years field experience.
Scope: Full code-level review of frontend + backend + exports + landing page against GST law and production readiness.

### GST-011 — Landing page contains unverifiable marketing claims

**Status:** deferred (Per user request 2026-03-18)

---

### GST-012 — "Invoice Value" column should read "Ledger Amount (Tax-Inclusive)"

**Status:** done

---

### GST-013 — "Est. ITC" column label is ambiguous

**Status:** done

---

### GST-014 — Show potential liability for AT_RISK rows (Early Warning)

**Priority:** P1 | **Area:** UI Display | **Status:** done

**Problem:**
For invoices in the 150–180 day window (AT_RISK), the engine sets current liability to zero. UI shows ₹0.00 which is misleading for a "Due Soon" warning.

**Fix (Chosen Strategy):**
1. Update `Rule37InterestCalculationService.java` to compute `itcAmount` even for `AT_RISK` status.
2. In UI, show this amount in "ITC @ 18%" and "Reversal" columns but mark as "Potential" or use distinct styling.
3. Keep Interest at ₹0 until 180 days are actually breached.


---

### GST-015 — GSTR-3B Summary export missing interest column

**Priority:** P1 | **Area:** Export | **Status:** done

**Problem:**
The GSTR-3B Summary Excel export (`Gstr3bSummaryExportStrategy.java`) only has two columns: "GSTR-3B Return Period" and "Rule 37 ITC Reversal Amount". It does not show the interest amount payable on the reversed ITC.

Per **Section 50(1) of the CGST Act**, interest is payable when ITC is wrongly availed and utilised. A CA filing GSTR-3B needs *both* the reversal amount (Table 4(B)(2)) and the interest amount (Table 5 of GSTR-3B) in the same report.

**Files:**
- `backend-service/.../Gstr3bSummaryExportStrategy.java` lines 45-46

**Fix:**
Add a third column: "Interest Payable (18% p.a.)" grouped by GSTR-3B period. Add a fourth column: "Total Liability (Reversal + Interest)". Add footer note citing Section 50(1) CGST Act.

---

### GST-016 — Excel export has no Invoice Number column

**Priority:** P1 | **Area:** Export | **Status:** done

**Problem:**
The `Rule37ExcelExportStrategy.java` ledger detail sheet headers (line 137) are:
`"Supplier", "Purchase Date", "Payment Date", "Principal Amount", "Delay Days", "ITC (18%)", "ITC to Reverse", "Interest (18% p.a.)", "Status", "Payment Deadline", "Risk Category", "GSTR-3B Period"`

Invoice Number is **missing** from the Excel export even though it is parsed, stored in `InterestRow.invoiceNumber`, and shown in the UI. A CA cannot reconcile with purchase register or GSTR-2B without it.

**Fix:**
Insert "Invoice/Voucher No." as column index 1 (after Supplier), shifting all other columns right.

---

### GST-017 — Typo in Rule 37 explainer

**Priority:** P2 | **Area:** Landing Page | **Status:** done

**Problem:**
Line 539 of `landing.component.html`: "Pay 18% interest on reversed amount if reversed **afetr** 180 days" — should be "after".

**Fix:**
Correct typo.

---

### GST-018 — "How Late" column header is informal

**Priority:** P1 | **Area:** UI Labels | **Status:** done

**Problem:**
The compliance-view table has a column header **"How Late"** (line 242 of `compliance-view.component.html`). This is too casual for a professional GST compliance tool. CAs and auditors expect formal terminology.

**Fix:**
Rename to **"Delay (Days)"** in the UI. The Excel export already uses "Delay Days" which is better.

---

### GST-019 — "File In" column header is unclear

**Priority:** P2 | **Area:** UI Labels | **Status:** done

**Problem:**
Column header says **"File In"** (line 249). This is ambiguous — a user won't know if it means a file name or a return period.

**Fix:**
Rename to **"Return Period"** or **"GSTR-3B Period"** for clarity. Match the Excel export label "GSTR-3B Period".

---

### GST-020 — Interest charged on PAID_LATE where payment is within 180 days but after ITC availment date

**Priority:** P1 | **Area:** Calculation Engine | **Status:** done

**Problem:**
The engine computes interest using `interestDays = daysBetween(itcAvailmentDate, paymentDate)` for PAID_LATE invoices (line 102 of `Rule37InterestCalculationService.java`). This means an invoice paid on Day 100 could still accrue interest if the ITC availment date (20th of next month after purchase) is before the payment date.

However, **Rule 37 only triggers reversal when payment is not made within 180 days**. If a supplier is paid within 180 days (even if after ITC availment), there is **no reversal** and therefore **no interest** under Section 50. The current logic incorrectly shows interest for invoices paid late (relative to ITC availment) but within the 180-day window.

**Legal basis:** Second proviso to Section 16(2) — reversal obligation arises only when the 180-day condition is breached. Interest under Section 50 applies only on the reversed amount.

**Impact:** This is over-reporting liability. CAs would flag this as incorrect.

**Fix:**
In `processFifoMatching`, the interest calculation should be:
- If `delayDays <= 180`: status = PAID_ON_TIME, interest = 0, itcAmount = 0 (no reversal, no interest)
- If `delayDays > 180`: status = PAID_LATE, compute ITC reversal and interest from ITC availment date to payment date

The current code DOES gate on `delayDays > DAYS_THRESHOLD` for PAID_LATE status (line 104), but the `interestDays` calculation is computed BEFORE this check (line 102), and both branches use the same `delayDays` variable that measures purchase-to-payment, not availment-to-payment. The ON_TIME branch at line 110 correctly sets interest to 0. **Verify** that no edge case leaks interest into on-time payments.

---

### GST-021 — Redundant "Est." prefix on every column

**Priority:** P1 | **Area:** UI / Professional | **Status:** done

**Problem:**
Multiple column headers use "Est." prefix: "Est. ITC", "Est. Reversal", "Est. Interest", "Est. Total". This clutters the UI and makes it look amateurish. Professional tools use a single disclaimer banner or footer note.

**Fix:**
1. Remove "Est." from all column headers
2. Add a single footnote/banner above the table: "All values are estimates based on uploaded ledger data at 18% GST. Verify with your CA before filing."
3. Keep the collapsible legal disclaimer panel at the bottom

---

### Issue Validation Matrix (Addendum)

| ID | Validation Result | Confidence | Why I am confident | Safe-fix regression guard |
|---|---|---|---|---|
| GST-011 | `Confirmed release blocker` | Very High | Visible in landing.component.html — fake testimonials, unverified claims | Remove claims; add flag for real user count |
| GST-012 | `Fixed and Verified` | Very High | Labels updated in HTML and Excel exports | Code review + CSS alignment |
| GST-013 | `Fixed and Verified` | Very High | ITC column now explicit with tooltips in UI | Manual verification of HTML |
| GST-014 | `Fixed and Verified` | Very High | createAtRiskRow now computes potential ITC correctly | Unit test verified |
| GST-015 | `Fixed and Verified` | Very High | GSTR-3B export now contains Interest and Liability columns | Excel generation manual pass |
| GST-016 | `Fixed and Verified` | Very High | Invoice Number added as first data column in Excel | Excel generation manual pass |
| GST-017 | `Fixed and Verified` | Very High | Typo corrected in landing page | Visual check of string |
| GST-018 | `Fixed and Verified` | Very High | Label renamed to "Delay (Days)" | UI alignment |
| GST-019 | `Fixed and Verified` | Very High | Label renamed to "Return Period" | UI alignment |
| GST-020 | `Verified Correct` | Very High | Calculation engine gating confirmed correct; unit test added for safety | Automated test guard |
| GST-021 | `Fixed and Verified` | Very High | Removed prefixes; added disclaimer banner + CSS | UI alignment |
