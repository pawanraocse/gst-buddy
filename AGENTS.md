---
name: gst-buddy-system
description: System-level rules for all agents working on the GSTBuddy project.
---

# GSTBuddy — Agent System Rules

## Output Rules (MANDATORY)

1. **Be terse.** Default response ≤ 5 lines unless code or a plan is required.
2. **No filler.** Never output "Great question!", "Let me help!", "Sure thing!" or similar.
3. **Code = delta only.** Show only changed lines with 2-3 lines of context. Never output full files for edits.
4. **No invented data.** Never fabricate GST rates, dates, amounts, metrics, or legal citations. If unsure, say "I need to verify" and check the `resources/RULE ENGINE/` PRDs or `gst-expert` skill.
5. **No explanations unless asked.** After code changes, state what changed and why in ≤ 2 sentences. Don't explain how the code works.
6. **Tables over paragraphs.** When comparing options or listing items, use tables.
7. **Verify before confirming.** Run linter/tests after code changes. Never say "done" without a green build.

## Anti-Hallucination Rules

- **GST formulas**: Always cross-check against `gst-expert` skill BEFORE outputting any rate, cap, date, or formula.
- **Legal citations**: Must be exact — `"Section X(Y), CGST Act 2017"`. Never approximate.
- **Codebase patterns**: Read `.agent-skills` and check existing code BEFORE creating new patterns. Never invent patterns that contradict the established codebase.
- **File paths**: Verify with `ls` or `grep` before referencing. Never guess a path.
- **When uncertain**: Say so. `"I'm not confident about [X] — let me verify."` is always better than a wrong answer.

## Memory Protocol

### Session Start (MANDATORY)
Read these files before any task:
```
.memory/AGENT_MEMORY.md → .memory/SESSION_LOG.md → .memory/DECISIONS.md →
.memory/TECH_CONTEXT.md → .memory/PATTERNS.md → .memory/TODO.md → .agent-skills
```
Confirm: **"✅ Memory loaded — [N] decisions, last session: [DATE]"**
If any file missing → create from `.memory/README.md` schema.

### Session End (MANDATORY)
```
APPEND  SESSION_LOG.md   ← 3-line session summary
UPDATE  DECISIONS.md     ← only if new architectural/legal decisions made
UPDATE  TODO.md          ← close done items, add new ones
UPDATE  others           ← only if changed (AGENT_MEMORY, TECH_CONTEXT, PATTERNS, .agent-skills)
```
Confirm: **"✅ Memory persisted — session [DATE] written."**

### Memory Rules
- `AGENT_MEMORY.md` ≤ 500 lines — prune oldest
- `SESSION_LOG.md` ≤ 10 sessions — rotate oldest
- Entries: `date · agent · confidence [HIGH|MED|LOW]`
- Never store secrets/tokens/PII
- **latest human-approved decision wins** on conflicts

## Skill Routing

Skills are loaded on-demand when triggered. Do NOT restate skill content in responses.

| Trigger | Skill |
|---|---|
| GST calculation, formula, rate, legal citation, tooltip text | `gst-expert` |
| Java/Spring Boot code, API, testing, Flyway, exceptions | `java-backend` |
| UI redesign, UX audit, layout, animation, color, typography | `ui-ux-expert` |
| Angular component, PrimeNG, theming, signals, forms | `angular-components` |

## Stack (reference — details in skills)
- Backend: Java 21, Spring Boot 3.5, PostgreSQL 16
- Frontend: Angular 21, PrimeNG 21 (Aura)
- Auth: AWS Cognito + Amplify
- Deploy: AWS EC2 (Mumbai, ap-south-1)

## Formatting
- Legal citations: `Section X(Y) of CGST Act, 2017`
- Amounts: Indian notation (₹1,23,456), always split CGST/SGST/IGST
- Dates: DD-MM-YYYY in UI/GST context, ISO 8601 in code
- GSTIN examples: mask as `29XXXXX1234X1ZX`
- Code: Java (backend), TypeScript (frontend) — always typed
