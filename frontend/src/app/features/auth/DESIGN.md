# Design System: GSTBuddies Auth Flow

## 1. Visual Theme & Atmosphere
A "Confidence-Driven" interface that transforms the stressful perception of taxes into a sense of calm and control. The atmosphere is **Balanced** (Density 5) with **Offset Asymmetric** (Variance 7) layouts. It prioritizes human connection and relief over cold compliance.

## 2. Color Palette & Roles
- **Canvas White** (#F9FAFB) — Primary background for the form panel
- **Pure Surface** (#FFFFFF) — Login card background
- **Deep Slate** (#0F172A) — Primary text (Zinc-950 depth)
- **Muted Indigo** (#6366F1) — Single accent for CTAs and focus states
- **Steel Mist** (#64748B) — Secondary text and descriptions
- **Soft Border** (#E2E8F0) — Subtle structural lines

## 3. Typography Rules
- **Display:** "Outfit" — Track-tight, controlled scale. Used for headlines to convey modern friendliness.
- **Body:** "Outfit" — Relaxed leading (1.6), 65ch max-width.
- **Mono:** "JetBrains Mono" — For technical labels or IDs.
- **Banned:** Inter (generic), standard system serifs (Times/Georgia).

## 4. Component Stylings
* **Buttons:** Tactile feel. Primary button uses Indigo gradient (#6366F1 to #4F46E5). No neon outer glows. Active state has a -1px vertical shift.
* **Cards:** Generously rounded (1.5rem). Very subtle diffused shadow (rgba(0,0,0,0.05)).
* **Inputs:** Labels above, focus ring in Indigo. High-contrast placeholders.
* **Imagery:** Full-bleed, high-resolution photography on the left panel. Subject should be people experiencing "Relief" or "Productive Happiness". No generic vector clip-art.

## 5. Layout Principles
- **Split-Screen Interaction:** 50/50 split on desktop. Left side is visual/emotional, Right side is functional.
- **Single-Column Collapse:** Below 768px, the visual panel disappears or becomes a small header, prioritizing the form.
- **Negative Space:** Use generous internal padding (3rem+) to prevent a cramped feeling.

## 6. Motion & Interaction
- **Entrance:** Staggered fade-in for the form fields.
- **Micro-interactions:** Subtle hover scaling on the primary button.
- **Loading:** Smooth pulse state on the button when authenticating.

## 7. Anti-Patterns (Banned)
- No generic AI-generated vector illustrations (clip-art style).
- No "Elevate your taxes" type clichés.
- No redundant titles (e.g., repeating the app name multiple times in one view).
- No pure black (#000000).
- No neon shadows.
