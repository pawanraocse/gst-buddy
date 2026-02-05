# GST Buddy - Frontend Design System

**Version:** 1.0
**Philosophy:** "Anti-Gravity" — Lightweight, Friendly, Trustworthy.
**Tech Stack:** Angular 21, PrimeNG, PrimeFlex, SCSS.

---

## 1. Brand Identity & Color Palette

We move away from "Admin Grey" to "Fintech Glass".

### Primary Colors (Trust & Action)
| Token | Color | Hex | Usage |
|-------|-------|-----|-------|
| `--primary-500` | **Indigo** | `#6366f1` | Primary Actions, Brand elements |
| `--primary-600` | Indigo Dark | `#4f46e5` | Hover states |
| `--primary-gradient` | Linear | `linear-gradient(135deg, #6366f1 0%, #a855f7 100%)` | Buttons, Active states |

### Status Colors (Friendliness)
| Token | Color | Hex | Usage |
|-------|-------|-----|-------|
| `--success-500` | **Teal** | `#14b8a6` | "All Clear", Success prompts |
| `--warning-500` | **Amber** | `#f59e0b` | "Action Pending", Attention needed |
| `--danger-500` | Rose | `#f43f5e` | Critical errors only (Use sparingly) |
| `--info-500` | Sky | `#0ea5e9` | Information, tooltips |

### Backgrounds (Anti-Gravity)
| Token | Value | Usage |
|-------|-------|-------|
| `--bg-app` | `#f8fafc` (Slate-50) | Main app background |
| `--bg-glass` | `rgba(255, 255, 255, 0.7)` | Cards, Panels (backdrop-filter: blur(10px)) |
| `--bg-overlay` | `rgba(255, 255, 255, 0.9)` | Modals, Dropdowns |

---

## 2. Typography

**Font Family:** `'Inter', sans-serif`

| Class | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| `.text-display` | 2.5rem | 700 | 1.2 | Hero headings, "You are all clear" |
| `.text-h1` | 1.75rem | 600 | 1.3 | Page titles |
| `.text-h2` | 1.5rem | 600 | 1.3 | Section headers |
| `.text-body` | 1rem | 400 | 1.5 | Standard text |
| `.text-sm` | 0.875rem | 400 | 1.5 | Helper text, secondary labels |

---

## 3. Micro-Interactions & Animations

We use GPU-accelerated CSS animations.

### Classes
*   `.fade-in-up`: Entrance animation for cards/content.
*   `.float-y`: Gentle floating effect for hero images.
*   `.scale-in`: Pop-in effect for badges/buttons.
*   `.hover-lift`: `transform: translateY(-2px)` on hover.

### Timing
*   `--ease-out-back`: `cubic-bezier(0.34, 1.56, 0.64, 1)` (Bouncy/Friendly)
*   `--transition-fast`: `0.2s`

---

## 4. Components Strategy (PrimeNG Overrides)

### Cards (`.glass-card`)
```scss
background: var(--bg-glass);
backdrop-filter: blur(12px);
border: 1px solid rgba(255, 255, 255, 0.5);
box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
border-radius: 1rem;
```

### Buttons (`.p-button-primary`)
*   **Shape:** Rounded pill or soft rectangle (`border-radius: 0.75rem`).
*   **Fill:** `--primary-gradient`.
*   **Shadow:** Colored shadow glow on hover.

### Inputs
*   **Border:** Soft Gray (`#e2e8f0`).
*   **Focus:** Indigo Ring (3px solid `#6366f120`).

---

## 5. Layout Patterns

1.  **Centered Float:** For simple tasks (Login, Upload). Content floats in center of a subtle animated background.
2.  **Dashboard Grid:** "Masonry-lite" grid using Flex/Grid. Status banner spanning full width on top.
3.  **Split View:** Sidebar (Glass) + Main Content (Scrollable).

