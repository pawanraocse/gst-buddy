# Audit Pipeline Architecture: From Rule-Centric to Document-Centric

## 1. The Core Problem: Why the Current Architecture Fails at Scale

Our current Audit Engine was built and validated during Phase 0 / 0.5 using a **Rule-Centric** execution model. 
While this worked for "Rule 37" and the initial "GSTR-1 Late Fee," it creates a massive scalability bottleneck as we add the remaining 16+ rules defined in our roadmap.

### 1.1 The Cognitive Load on the CA
Currently, the system forces the CA (Chartered Accountant) to do the mental heavy lifting. If a CA wants to analyze a client's compliance, they navigate to the `/app/gstr1-late-fee` module, upload a GSTR-1, and run the calculation. 
- But what if they also need to check the exact same GSTR-1 for **Place of Supply (POS) issues**? They would have to navigate to a new screen, re-upload the file, and run that rule.
- CAs don't think in terms of specific CGST rule sections (e.g., "Let me run Section 47(1) today"). They think in terms of documents: *"Here is my client's GSTR-1 and GSTR-3B for April. Tell me what's wrong."*

### 1.2 The Orchestrator Bottleneck
Presently, our `AuditRunOrchestrator` methods (like `processGstrUpload`) hardcode the exact rule to run:
```java
// Anti-Pattern: Hardcoded rule execution
AuditRule rule = ruleRegistry.getRule("LATE_FEE_GSTR1");
AuditRuleResult ruleResult = rule.execute(input, ctx);
```
With 18+ rules, this means we would be writing 18+ different orchestrator methods and 18+ different API endpoints.

### 1.3 The Cross-Document Impossibility
Phase 2 of our roadmap introduces **Reconciliation Rules** (e.g., GSTR-1 vs GSTR-3B vs GSTR-9). 
In a Rule-Centric model, there is no shared context where multiple documents can live side-by-side to be evaluated. If rules are executed in complete isolation, matching a GSTR-1 against a GSTR-3B is impossible.

---

## 2. The Solution We Need to Think For: A State Machine / Pipeline

To achieve the vision of becoming a "Single Source of Truth for GST Audits," we must invert the control. The engine must become a **Document-Centric State Machine**.

The user uploads a document (or batch of documents). The system parses them, figures out what they are, auto-discovers every applicable legal rule, and runs them automatically.

### 2.1 The "Audit Context" as a Stateful Session
Instead of treating an audit as a stateless single-file transaction, we must upgrade `AuditContext` so it holds a basket of parsed documents.
- **Before:** `AuditContext` only held `tenantId`, `userId`, `financialYear`.
- **After:** `AuditContext` acts as a session wrapper. It contains a `List<AuditDocument>`. When a user uploads a GSTR-3B, we add a `DocumentType.GSTR_3B` JSON to the context.

### 2.2 Inversion of Control: Rule Auto-Discovery
Rules must no longer be blindly invoked by the orchestrator. Instead, every rule must declare its data requirements.
We introduce a mechanism where the Orchestrator presents the `AuditContext` to the Registry and asks: *"Which rules are satisfied by the documents currently present in this context?"*

If the user uploads:
- **GSTR-1 only:** The engine selects `Gstr1LateFeeRule` and `PosValidationRule`.
- **GSTR-3B only:** The engine selects `Gstr3BLateFeeRule`, `Gstr3BInterestRule`, and `Rule86BRestrictionRule`.
- **GSTR-1 + GSTR-3B:** The engine selects *all* of the above, **plus** dynamically unlocks the `Gstr1Vs3bReconciliationRule`.

### 2.3 The Execution Pipeline (Loop)
The Orchestrator stops hardcoding `ruleRegistry.getRule("X")`. Instead, it operates a pipeline loop:
1. `rules = registry.getExecutableRules(auditContext)`
2. `List<AuditFinding> allFindings = new ArrayList<>();`
3. `for (rule in rules) { allFindings.addAll( rule.execute(auditContext) ) }`
4. Store the results in the database under a single unified `AuditRun` with a `rules_executed` array (e.g., `["LATE_FEE_GSTR3B", "INTEREST_GSTR3B"]`).

### 2.4 Unified UI Reporting 
The frontend shouldn't ask the user to pick rules either. The UI changes to a generic "Compliance Analyzer" drop-zone.
When the backend returns the `UploadResult`, the frontend table automatically displays a comprehensive "Statement", grouping risks from multiple rules side-by-side (Late Fee risks right next to RCM risks). *Our frontend `gstr1-results.component` is actually already generic enough to handle this, because it loops over `findingsSummary`.*

---

## 3. High-Level Technical Data Flow

1. **User Request (`POST /api/v1/audit/analyze`)**
   - User uploads `GSTR-3B_April2024.pdf`.
2. **Parser Stage**
   - The Python sidecar ingests the PDF. It uses file headers/layout to determine `doc_type = GSTR_3B`.
   - Returns extracted JSON.
3. **Context Hydration**
   - Orchestrator creates `AuditDocument(GSTR_3B, parsedJson)`.
   - Wraps it into `AuditContext`.
4. **Rule Resolution**
   - Orchestrator asks `AuditRuleRegistry.findExecutable(context)`.
   - Both `GSTR3B_LateFeeRule` & `GSTR3B_InterestRule` return `canExecute() == true`.
5. **Multi-Rule Execution**
   - `GSTR3B_LateFeeRule.execute()` -> Returns 1 Finding.
   - `GSTR3B_InterestRule.execute()` -> Returns 3 Findings.
6. **Aggregation & Persistence**
   - 1 Database `AuditRun` record is created.
   - 4 `AuditFinding` records are spawned.
   - Credits deducted = 2 (1 per rule executed).
7. **Response & Display**
   - The UI displays a single clean statement: *"4 Risks Detected in April 2024 GSTR-3B"* and breaks it down by severity. 
