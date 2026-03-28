# GSTbuddies — UI/UX Design System Upgrade Strategy

**Version:** 2.0 — "Fintech Glass → Fintech Crystal"  
**Prepared by:** Design Architecture Team  
**Date:** Feb 2026  
**Philosophy:** Elevate from "Fintech Glass" to **"Fintech Crystal"** — Where every surface feels sculpted, every interaction feels intentional, and every screen builds trust.

---

## Executive Summary

GSTbuddies's current design system (v1.0) has a solid foundation — glassmorphism cards, Indigo/Violet palette, Inter + Clash Display typography, and GPU-accelerated animations. However, to compete visually with Stripe, Linear, and Vercel, the system needs deeper refinement across **six dimensions**: typography hierarchy, color depth, iconography consistency, motion choreography, component polish, and visual asset strategy.

This document provides a production-ready upgrade plan across all six.

---

## 1. Typography System — "Crystal Scale"

### Current State
- **Fonts:** Clash Display (headings) + General Sans (body) + JetBrains Mono (code)
- **Issue:** Font pairing is strong but the typographic scale lacks intermediate steps. Display sizes jump abruptly. No optical sizing adjustments.

### Upgrade Strategy

| Token | Size | Weight | Line Height | Letter Spacing | Usage |
|---|---|---|---|---|---|
| `--text-hero` | 4rem / 64px | 700 | 1.05 | -0.03em | Landing hero headline only |
| `--text-display` | 2.5rem / 40px | 700 | 1.15 | -0.025em | Section headings, "You are all clear" |
| `--text-h1` | 1.875rem / 30px | 600 | 1.25 | -0.02em | Page titles (Dashboard, Admin) |
| `--text-h2` | 1.5rem / 24px | 600 | 1.3 | -0.015em | Section headers, card titles |
| `--text-h3` | 1.25rem / 20px | 600 | 1.35 | -0.01em | Sub-sections, panel headers |
| `--text-body-lg` | 1.125rem / 18px | 400 | 1.6 | 0 | Hero subtitles, key paragraphs |
| `--text-body` | 1rem / 16px | 400 | 1.6 | 0 | Standard body text |
| `--text-sm` | 0.875rem / 14px | 400 | 1.5 | 0.005em | Helper text, table data |
| `--text-xs` | 0.75rem / 12px | 500 | 1.5 | 0.02em | Badges, labels, overline text |
| `--text-mono` | 0.875rem / 14px | 400 | 1.5 | 0.05em | Code, IDs, API responses |

### Key Decisions
- **Negative letter-spacing** on headings (like Linear/Vercel) creates visual density and premium feel
- **Font display:** All fonts load with `font-display: swap` and preconnect tags
- **Tabular numbers:** Enable `font-variant-numeric: tabular-nums` for all financial figures (₹ amounts, percentages) so columns align perfectly

---

## 2. Color System — "Crystal Depth"

### Current State
Solid Indigo/Violet primary palette with Tailwind-influenced semantic colors. Good but lacks **depth layers** and **dark mode readiness**.

### Upgrade: Extended Palette with Alpha Layers

#### Primary (Indigo — Trust)
| Token | Value | Usage |
|---|---|---|
| `--primary-50` | `#eef2ff` | Hover backgrounds, active sidebar |
| `--primary-100` | `#e0e7ff` | Selected states, badges |
| `--primary-200` | `#c7d2fe` | Focus rings |
| `--primary-500` | `#6366f1` | Primary actions, brand |
| `--primary-600` | `#4f46e5` | Hover/pressed |
| `--primary-700` | `#4338ca` | Active/focus |
| `--primary-alpha-10` | `rgba(99, 102, 241, 0.10)` | Subtle tints behind icon containers |
| `--primary-alpha-20` | `rgba(99, 102, 241, 0.20)` | Focus rings, glow effects |

#### Surface System (Light Mode)
| Token | Value | Usage |
|---|---|---|
| `--surface-0` | `#ffffff` | Cards, modals |
| `--surface-50` | `#f8fafc` | App background |
| `--surface-100` | `#f1f5f9` | Nested panels, input backgrounds |
| `--surface-200` | `#e2e8f0` | Dividers, borders |
| `--surface-glass` | `rgba(255, 255, 255, 0.72)` | Glass cards (with blur) |
| `--surface-glass-hover` | `rgba(255, 255, 255, 0.85)` | Glass hover state |

#### Surface System (Dark Mode — Prepared)
| Token | Value | Usage |
|---|---|---|
| `--surface-0-dark` | `#0f172a` | Cards, modals |
| `--surface-50-dark` | `#020617` | App background |
| `--surface-100-dark` | `#1e293b` | Nested panels |
| `--surface-200-dark` | `#334155` | Dividers |
| `--surface-glass-dark` | `rgba(15, 23, 42, 0.80)` | Glass cards (dark) |
| `--text-main-dark` | `#f1f5f9` | Primary text |
| `--text-secondary-dark` | `#94a3b8` | Secondary text |

### Key Decisions
- **Alpha tokens** eliminate hardcoded `rgba()` scattered across components
- **Dark mode** tokens are defined now (CSS custom properties with `prefers-color-scheme`) even if toggle isn't built yet — this prevents tech debt
- **Gradient refinement:** Primary gradient gets a subtle midpoint stop: `linear-gradient(135deg, #6366f1 0%, #7c3aed 50%, #a855f7 100%)` for richer depth

---

## 3. Spacing & Layout Tokens

### Spacing Scale (4px base unit)

| Token | Value | Usage |
|---|---|---|
| `--space-0` | `0` | Reset |
| `--space-1` | `0.25rem` (4px) | Inline gaps |
| `--space-2` | `0.5rem` (8px) | Icon-to-label gaps |
| `--space-3` | `0.75rem` (12px) | Small component padding |
| `--space-4` | `1rem` (16px) | Standard card padding |
| `--space-5` | `1.25rem` (20px) | Section gaps |
| `--space-6` | `1.5rem` (24px) | Card internal padding |
| `--space-8` | `2rem` (32px) | Section margins |
| `--space-10` | `2.5rem` (40px) | Between major sections |
| `--space-12` | `3rem` (48px) | Page top/bottom padding |
| `--space-16` | `4rem` (64px) | Landing section spacing |
| `--space-20` | `5rem` (80px) | Hero padding |

### Elevation / Shadow System

| Token | Value | Context |
|---|---|---|
| `--shadow-xs` | `0 1px 2px rgba(0,0,0,0.04)` | Inputs, subtle borders |
| `--shadow-sm` | `0 2px 4px rgba(0,0,0,0.06)` | Small cards, badges |
| `--shadow-md` | `0 4px 12px rgba(0,0,0,0.08)` | Cards, panels |
| `--shadow-lg` | `0 8px 24px rgba(0,0,0,0.10)` | Modals, popovers |
| `--shadow-xl` | `0 16px 48px rgba(0,0,0,0.12)` | Sidebar, floating elements |
| `--shadow-glow-primary` | `0 8px 20px rgba(99,102,241,0.25)` | Primary button hover |
| `--shadow-glow-success` | `0 8px 20px rgba(20,184,166,0.25)` | Success state hover |

---

## 4. Icon Strategy — "Crystal Icons"

### Current State
The app uses **PrimeIcons** (`pi pi-*`) for everything: navigation, actions, status indicators, and decorative elements. PrimeIcons is functional but visually inconsistent — some icons feel heavy, others thin, and the set lacks domain-specific icons (e.g., GST shield, Tally logo, INR currency).

### Recommendation: Hybrid Icon System

| Layer | Library | Usage | Reasoning |
|---|---|---|---|
| **System Icons** | **Lucide Icons** | Navigation, actions, UI controls (search, settings, close, chevrons, menu) | Consistent 24×24 grid, 1.5px stroke, 200+ icons, tree-shakeable, MIT license. Visually matches Inter/General Sans. |
| **Status Icons** | **Custom SVG Set** (AI-generated) | Compliance status (All Clear, Action Pending, Critical), rule badges (Rule 37, 36(4)), metric icons | Brand-specific, ensures visual uniqueness. Generated via Midjourney/DALL·E then refined in Figma. |
| **Decorative** | **AI-generated illustrations** | Landing page section icons, hero companion, empty states | Full control over brand tone. See Asset Generation Prompt Library. |
| **Fallback** | **PrimeIcons** | Any remaining PrimeNG-specific icons (p-dialog close, p-accordion chevron, etc.) | Keep for PrimeNG component internals only. Don't use for custom UI. |

### Icon Usage Rules

| Context | Size | Stroke | Color | Example |
|---|---|---|---|---|
| **Navigation** | 20px | 1.5px | `--text-secondary` → `--primary-600` on active | Sidebar menu items |
| **Inline Actions** | 16px | 1.5px | `--text-secondary` | Edit, delete, download buttons |
| **Status Indicators** | 24px | 2px | Semantic color (`--success-500`, `--warning-500`) | ✓ All Clear badge |
| **Feature Cards** | 32px | - | Primary gradient fill | Landing bento grid icons |
| **Hero / Decorative** | 48-64px | - | Full-color illustration | Landing hero, empty states |
| **Badge/Label** | 14px | 1.5px | Inherit from text | Inline tags, credit labels |

### Migration Plan
1. Install Lucide Angular: `npm install lucide-angular`
2. Create `shared/icons/` module wrapping Lucide with app-specific defaults
3. Replace `pi pi-*` in layout, dashboard, auth components (batch migration)
4. Keep PrimeIcons as peer dependency for internal PrimeNG component rendering

---

## 5. Motion & Micro-Interaction Philosophy — "Crystal Motion"

### Principles
1. **Purposeful, not gratuitous** — Every animation must communicate state change, hierarchy, or spatial relationship
2. **Choreographed, not chaotic** — Staggered entrances, not everything at once
3. **Fast by default** — 200ms for micro-interactions, 300-400ms for layout changes
4. **GPU-friendly** — Only animate `transform` and `opacity` (never `width`, `height`, `margin`)

### Animation Tokens

| Token | Value | Usage |
|---|---|---|
| `--ease-default` | `cubic-bezier(0.2, 0, 0, 1)` | Standard transitions (Material 3 standard) |
| `--ease-spring` | `cubic-bezier(0.34, 1.56, 0.64, 1)` | Bouncy entrance for badges, toasts |
| `--ease-out` | `cubic-bezier(0, 0, 0.2, 1)` | Elements leaving the screen |
| `--duration-instant` | `100ms` | Color/opacity hover changes |
| `--duration-fast` | `200ms` | Button press, toggle, tooltip |
| `--duration-normal` | `300ms` | Card entrance, modal open |
| `--duration-slow` | `500ms` | Page transitions, large layout shifts |

### Interaction Catalog

| Interaction | Animation | Duration | Easing |
|---|---|---|---|
| **Button hover** | `translateY(-1px)` + shadow glow | 200ms | `--ease-default` |
| **Button press** | `scale(0.97)` | 100ms | `--ease-default` |
| **Card entrance** | `fadeInUp` (opacity 0→1, translateY 20→0) | 400ms | `--ease-spring` |
| **Card hover** | `translateY(-4px)` + shadow-lg | 200ms | `--ease-default` |
| **Sidebar menu hover** | `translateX(4px)` + color change | 150ms | `--ease-default` |
| **Tab switch** | Underline `scaleX(0→1)` from center | 250ms | `--ease-default` |
| **Toast/snackbar** | `slideInRight` + `fadeIn` | 300ms | `--ease-spring` |
| **Modal open** | Overlay `fadeIn` + content `scale(0.95→1)` + blur | 300ms | `--ease-default` |
| **Loading spinner** | Indigo gradient ring rotation | 800ms | linear |
| **Skeleton loading** | Shimmer gradient sweep | 1.5s | linear, infinite |
| **Empty state** | Gentle `float-y` (translateY ±8px) | 3s | ease-in-out, infinite |
| **Status badge appear** | `scale(0→1)` + `fadeIn` | 300ms | `--ease-spring` |
| **Number count-up** | Eased incremental | 2000ms | ease-out-quad |

### New Animations to Add

```scss
// Skeleton shimmer for loading states
@keyframes shimmer {
    0% { background-position: -200% 0; }
    100% { background-position: 200% 0; }
}

.skeleton {
    background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: var(--border-radius-md);
}

// Staggered entrance (used with @for loops)
.stagger-enter {
    @for $i from 1 through 12 {
        &:nth-child(#{$i}) {
            animation-delay: #{$i * 0.05}s;
        }
    }
}

// Subtle float for decorative elements
@keyframes float-y {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-8px); }
}

// Scale-in for badges and status indicators
@keyframes scale-in {
    from { transform: scale(0); opacity: 0; }
    to { transform: scale(1); opacity: 1; }
}
```

---

## 6. Component-Level Uplift Recommendations

### 6.1 Sidebar (`app-layout.component`)
| Aspect | Current | Upgrade |
|---|---|---|
| Logo | `pi pi-bolt` icon in gradient box | Replace with **custom SVG logomark** (GSTbuddies shield/calculator icon). Use `<img>` or inline SVG. |
| Menu items | `pi pi-*` icons, 1rem gap | Switch to Lucide icons at 20px, add `transition: all 200ms --ease-default` |
| Active state | Left border + background gradient | Add **animated indicator pill** (small rounded bar that slides vertically between items with `transform: translateY()`) |
| User card | Basic avatar + logout | Add **role badge** with colored dot (Admin = violet, User = teal). Add subtle ring animation around avatar on hover. |
| Footer area | Plain divider | Add **credit balance mini-widget** showing remaining credits with radial progress ring |

### 6.2 Dashboard (`dashboard.component`)
| Aspect | Current | Upgrade |
|---|---|---|
| Page title | Plain `h1` | Add **animated greeting** that changes by time of day: "Good morning, Rajesh 🌅" |
| Credit wallet badge | Inline with header | Redesign as **floating glass pill** with animated fill bar. When credits are low, pill pulses amber. |
| Status banner | Flat colored div | Add **gradient mesh background** (teal for success, amber for warning) with subtle particle effect like hero section |
| Tab toggle | Simple pill buttons | Replace with **segmented control** (iOS-style) with sliding background indicator |
| How-it-works panel | PrimeNG p-panel | Redesign as **horizontal step cards** with connecting line (like Stripe's checkout flow guide) |
| Upload area | Basic component | Add **drag-and-drop visual**: dashed border animates to solid on hover, file icon scales up, success state shows green checkmark animation |
| Processing state | `pi pi-spin pi-spinner` | Replace with **branded Lottie animation**: documents being analyzed with particle effects |
| Verdict banner | Flat card | Add **celebration confetti animation** for "All Clear" state (lightweight CSS/canvas confetti, not a library) |

### 6.3 Landing Page (`landing.component`)
| Aspect | Current | Upgrade |
|---|---|---|
| Hero illustration | `ai-companion` div is **empty** (image commented out) | Generate and add **3D isometric illustration** of a friendly robot holding a GST document with green checkmark |
| Feature icons | Mix of PrimeIcons and PNGs | Standardize all to **AI-generated isometric icons** at 64×64px with consistent style |
| Testimonial avatars | Letter initials (`{{ name.charAt(0) }}`) | Generate **stylized avatar illustrations** (not photos) in consistent art style |
| Comparison table | Basic HTML table | Add **row-by-row hover highlight** and animated checkmark entries on scroll |
| Pricing cards | Functional but flat | Add **glassmorphism depth** with `backdrop-filter`, popular card gets **animated glow border** |
| Video modal | "Coming Soon" placeholder | Replace with actual **product walkthrough video** (see Asset Generation Prompt Library for Sora/Kling prompts) |
| Footer | Standard grid | Add **subtle gradient mesh background** to ground the page and create visual closure |

### 6.4 Auth Pages — Split-Panel Strategy (`login, signup, verify-email, pwd-reset`)

> [!IMPORTANT]
> The auth screens currently feel **incomplete** — a centered glass card floating on an empty gradient background. There's no brand reinforcement, trust signals, or visual storytelling. This is the biggest visual gap compared to competitors like Stripe, Linear, and Vercel.

#### Proposed Solution: **Split-Panel Auth Layout**

Convert from "centered card" → **50/50 split layout** (desktop). Left panel = brand storytelling. Right panel = form.

```
┌─────────────────────────┬──────────────────────────┐
│                         │                          │
│   🎨 BRAND PANEL        │   📝 FORM PANEL          │
│                         │                          │
│   Hero Illustration     │   Glass Card with Form   │
│   Trust Signals         │   Email / Password       │
│   Animated Background   │   Google SSO             │
│   Rotating Testimonial  │   Links                  │
│                         │                          │
└─────────────────────────┴──────────────────────────┘
```

**On mobile:** Brand panel collapses to a compact header (logo + tagline + illustration shrunk to 150px), form panel takes full width.

#### Left Panel (Brand) — Per Screen

| Screen | Illustration | Headline | Sub-copy | Trust Signal |
|---|---|---|---|---|
| **Login** | AI Robot waving | "Welcome back to peace of mind" | "Your GST compliance is just one login away" | "Trusted by 500+ businesses" |
| **Signup** | AI Robot holding checkmark | "Join the smarter way to handle GST" | "Create your account in 30 seconds. No credit card required." | "5 free compliance checks" |
| **Verify Email** | Envelope with sparkles | "Almost there!" | "We've sent a magic code to your inbox" | "Your data is encrypted end-to-end" |
| **Forgot Password** | Lock with key floating | "Don't worry, we've got you" | "Reset your password in two quick steps" | "Bank-grade security" |

#### Right Panel (Form) Improvements

| Aspect | Current | Upgrade |
|---|---|---|
| Glass card | Good foundation | Add **subtle noise texture overlay** (`noise-texture.png`) for premium frosted feel |
| Error states | PrimeNG message | Add **shake animation** (`@keyframes shake` — `translateX(±5px)` for 300ms) on form card when validation fails |
| Success states | Redirect only | Add **checkmark Lottie animation** (300ms) before redirect |
| Password strength | Not visible on signup | Add **real-time strength meter** — gradient bar (rose→amber→teal→indigo) below password input |
| `pwd-reset` | Uses `p-card` + `bg-gray-100` — **inconsistent** with other auth pages | Migrate to shared `auth-page-container` + `auth-card` + animated blobs background. Apply glassmorphism consistently. |

#### New Auth-Specific Animations

```scss
// Shake on validation error
@keyframes shake {
    0%, 100% { transform: translateX(0); }
    10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
    20%, 40%, 60%, 80% { transform: translateX(5px); }
}

.auth-card.shake {
    animation: shake 0.4s ease;
}

// Gradient hue-shift for auth background blobs
@keyframes hue-rotate-slow {
    0% { filter: blur(60px) hue-rotate(0deg); }
    100% { filter: blur(60px) hue-rotate(30deg); }
}

.circle {
    animation: float-blob 20s ease-in-out infinite, 
               hue-rotate-slow 15s ease-in-out infinite alternate;
}
```

#### New Assets Required for Auth

| Asset | Type | Dimensions | Placement |
|---|---|---|---|
| `auth-login-illustration.png` | 3D Isometric | 400×400px WebP | Login left panel |
| `auth-signup-illustration.png` | 3D Isometric | 400×400px WebP | Signup left panel |
| `auth-verify-illustration.png` | 3D Isometric | 300×300px WebP | Verify email left panel |
| `auth-reset-illustration.png` | 3D Isometric | 300×300px WebP | Password reset left panel |
| `auth-bg-pattern.svg` | Subtle dot grid pattern | Tileable | Behind brand panel |

### 6.5 Admin Panel (`admin-*.component`)
| Aspect | Current | Upgrade |
|---|---|---|
| Data tables | Default PrimeNG | Add **row hover lift**, alternating subtle backgrounds, sticky header with blur |
| Stats cards | Functional | Add **sparkline mini-charts** (tiny inline SVG charts showing 7-day trend) |
| Actions | Icon buttons | Add **context menu** with smooth slide-in animation |
| Filters | Basic inputs | Add **filter chip pills** with animated remove (scale out + fade) |

---

## 7. Image, Illustration & Video Usage Guidelines

### Visual Style: "Crystal Isometric"
All custom illustrations follow a **consistent isometric 3D style** with:
- **Soft ambient lighting** (no harsh shadows)
- **Indigo (#6366f1) and Violet (#8b5cf6)** as dominant colors with teal accents
- **Rounded edges** on all 3D objects (matching `border-radius: 1rem` of the UI)
- **Transparent/gradient backgrounds** so they layer cleanly on glass surfaces
- **No photographic elements** — everything is illustrated/rendered

### Where Images Appear

| Location | Asset Type | Spec | Notes |
|---|---|---|---|
| **Landing Hero** | 3D Isometric Illustration | 800×600px, WebP, <100KB | Robot + document + checkmark, transparent BG |
| **Landing Rule Cards** | Isometric Icons | 128×128px, WebP, <20KB each | One per GST rule (Rule 37, 36(4), 86B, 16(4)) |
| **Landing Feature Icons** | Isometric Icons | 96×96px, WebP, <15KB each | Bolt, Shield, Dashboard, Sync |
| **Landing Steps** | Isometric Scene | 200×200px, WebP, <30KB each | Export, Upload, Results |
| **Dashboard Empty State** | Illustration | 400×300px, WebP, <50KB | Friendly robot sitting with "No data yet" |
| **Loading State** | Lottie JSON | <50KB | Document being scanned with particles |
| **Auth Background** | Gradient mesh | CSS-only | No image file needed — pure CSS animation |
| **Success Celebration** | Lottie JSON | <30KB | Green confetti burst |
| **Error State** | SVG inline | <5KB | Red exclamation with subtle shake |
| **Pricing Section BG** | Gradient texture | WebP or CSS | Dark gradient mesh with grain |

### Performance Rules
1. **All images served as WebP** with PNG fallback via `<picture>` element
2. **Lazy-load** everything below the fold (`loading="lazy"`)
3. **Eagerly load** hero illustration and above-fold icons (`loading="eager"`)
4. **Lottie animations** use `lottie-web/light` build (70KB vs 250KB full)
5. **No images > 100KB** — compress aggressively, target <50KB for icons
6. **Serve from same domain** (Angular assets) — no external CDN for initial load
7. **SVG for simple icons** — only use raster for complex illustrations

---

## 8. Summary: Priority Matrix

| Priority | Component | Effort | Impact |
|---|---|---|---|
| 🔴 P0 | Hero illustration (currently empty) | Medium | Very High — first impression |
| 🔴 P0 | Replace PrimeIcons with Lucide in layout | Medium | High — visual consistency |
| 🔴 P0 | Dashboard loading state (Lottie) | Low | High — perceived performance |
| 🟡 P1 | Skeleton loading screens | Medium | High — UX polish |
| 🔴 P0 | **Auth split-panel layout + illustrations** | Medium | **Very High** — every user sees auth first |
| 🔴 P0 | Password reset consistency (`pwd-reset`) | Low | High — currently inconsistent styling |
| 🟡 P1 | Auth page animated hue-shift backgrounds | Low | Medium — premium feel |
| 🟡 P1 | Pricing card glow + glass polish | Low | Medium — conversion |
| 🟡 P1 | Dark mode CSS tokens | Medium | Medium — future-proof |
| 🟢 P2 | Custom logomark SVG | Medium | Medium — brand identity |
| 🟢 P2 | Admin sparkline charts | High | Low-Medium — power users |
| 🟢 P2 | Product walkthrough video | High | High — landing conversion |

---

## 9. Screen-by-Screen Token Mapping

This appendix connects every design token to the specific screens and Angular components that use it.

### Typography Tokens → Screens

| Token | Landing Page | Dashboard | Auth (Login/Signup) | Admin Panel | Settings | Compliance View |
|---|---|---|---|---|---|---|
| `--text-hero` | ✅ Hero headline | — | — | — | — | — |
| `--text-display` | Section headers (Features, Pricing, FAQ) | ✅ Status banner "All Clear" | — | — | — | — |
| `--text-h1` | — | ✅ "GSTbuddies" page title | ✅ "Welcome to GSTbuddies" / "Create Account" | ✅ "Admin Dashboard" | ✅ "Account Settings" | — |
| `--text-h2` | Rule cards section titles | ✅ Verdict banner "Action Needed" | — | ✅ Section headers (Users, Credits) | ✅ Section headers | ✅ Ledger name |
| `--text-h3` | Feature card titles, FAQ questions | How-it-works step titles | — | ✅ Table cell group headers | — | ✅ Invoice summary headers |
| `--text-body-lg` | Hero subtitle, testimonial quotes | — | ✅ "Tax shouldn't be scary..." subtitle | — | — | — |
| `--text-body` | Feature descriptions, FAQ answers | ✅ Status descriptions, helper text | ✅ Form labels, footer links | ✅ Table data, descriptions | ✅ Form field labels | ✅ Invoice row data |
| `--text-sm` | Trust signals, badge labels, plan features | ✅ Credit count label, file count | ✅ "Forgot password?", "Don't have an account?" | ✅ Table metadata, timestamps | ✅ Helper text below inputs | ✅ Date, amount columns |
| `--text-xs` | Price unit ("one-time"), section labels | ✅ Credit badge "Low" label | — | ✅ Role badges, status pills | — | ✅ Column headers |
| `--text-mono` | — | — | — | ✅ User IDs, API keys | — | ✅ Invoice numbers, GSTIN |

### Color Tokens → Screens

| Token | Usage Across Screens |
|---|---|
| `--primary-500` | CTA buttons (all screens), sidebar active item, focus rings |
| `--primary-gradient` | Logo box, CTA buttons, auth icon ring, pricing "Best Value" |
| `--success-500` | "All Clear" badge (dashboard), "✓ Compliant" (landing rules), verify success |
| `--warning-500` | "Action Pending" banner (dashboard), "Low credits" pill, "Coming Soon" badge |
| `--error-500` | Form validation errors (auth), "Critical" status (compliance) |
| `--surface-glass` | Sidebar, header bar, auth card, landing feature cards, pricing cards |
| `--surface-50` | App background (dashboard, admin, settings), compliance section backgrounds |
| `--surface-100` | Input backgrounds (auth, settings), nested panels (dashboard how-it-works), table alternating rows |

### Shadow Tokens → Components

| Token | Components |
|---|---|
| `--shadow-xs` | Form inputs (auth, dashboard date picker, settings) |
| `--shadow-sm` | Credit wallet badge, status pills, tab toggles |
| `--shadow-md` | Glass cards (dashboard, landing features, pricing), auth card default |
| `--shadow-lg` | Auth card hover, modals (video, how-to), landing hero floating cards |
| `--shadow-xl` | Sidebar, mobile overlay sidebar, landing hero mockup |
| `--shadow-glow-primary` | Primary CTA hover (all screens), pricing popular card |
| `--shadow-glow-success` | "All Clear" verdict banner hover |

### Animation Tokens → Contexts

| Animation | Where Used |
|---|---|
| `fadeInUp` | Dashboard card entrance, landing section scroll-in, pricing cards |
| `fadeIn` | Route transition wrapper, modal overlays, auth background blobs |
| `slideInRight` | Toast notifications (global), mobile sidebar entrance |
| `shake` | Auth form validation error |
| `scale-in` | Status badges (dashboard verdict), landing "Live" badges |
| `shimmer` | Skeleton loading (dashboard initial load, admin tables, compliance data) |
| `float-y` | Landing hero floating cards, empty state illustration |
| `pulse` | Icon ring (auth), loading states, credit low warning |
| `confetti` | Dashboard "All Clear" celebration, auth signup success |

---

> [!TIP]
> **The two highest-impact changes are:**
> 1. **Hero illustration** — currently commented out, hero feels incomplete
> 2. **Auth split-panel layout** — every user sees auth first; adding brand storytelling + illustrations will dramatically improve first impression
>
> See the AI Asset Generation Prompt Library for production-ready prompts for both.
