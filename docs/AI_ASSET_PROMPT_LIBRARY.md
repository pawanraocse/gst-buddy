# AI Asset Generation Prompt Library

**Project:** GST Buddy  
**Version:** 1.0  
**Date:** Feb 2026  
**Visual Direction:** Crystal Isometric — Soft 3D, Indigo/Violet dominant, rounded forms, transparent backgrounds  

---

## Table of Contents

1. [Model Recommendations](#1-model-recommendations)
2. [Hero Illustrations](#2-hero-illustrations)
3. [GST Rule Icons (128×128)](#3-gst-rule-icons)
4. [Feature Icons (96×96)](#4-feature-icons)
5. [Step Illustrations (200×200)](#5-step-illustrations)
6. [UI Backgrounds & Textures](#6-ui-backgrounds--textures)
7. [Lottie / GIF Animations](#7-lottie--gif-animations)
8. [Product & Demo Videos](#8-product--demo-videos)
9. [Avatars & Testimonials](#9-avatars--testimonials)

---

## 1. Model Recommendations

| Asset Type | Recommended Tool | Reasoning |
|---|---|---|
| **3D Isometric Illustrations** | **Midjourney v6.1** | Best at consistent stylized 3D scenes with coherent lighting |
| **Flat/Outlined Icons** | **DALL·E 3** | Strong at isolated icon subjects on transparent backgrounds |
| **Animated Icons (GIF)** | **Midjourney v6.1 + Photoshop/ezgif** | Generate key frames in MJ, compile as lightweight GIF |
| **Gradient Meshes / Textures** | **Midjourney v6.1** | Excels at abstract generative art and color compositions |
| **Lottie Animations** | **LottieFiles AI + After Effects** | Generate base keyframes in LottieFiles, refine in AE |
| **GIF Loaders** | **Rive** or **LottieFiles AI** | Lightweight interactive animations, exportable as JSON |
| **Product Videos (short)** | **Sora** | Best for synthetic product walkthrough footage |
| **Feature Explainer Videos** | **Kling 1.6** | Good at scene-to-scene transitions with text overlays |
| **Avatar Illustrations** | **DALL·E 3** | Consistent style for portrait illustrations |

---

## 2. Hero Illustrations

### 2.1 Landing Page Hero — Main Illustration

**Filename:** `hero-illustration.png`  
**Dimensions:** 800×600px → Export as WebP (<100KB)  
**In-App Location:** Landing page, hero section right side (`div.ai-companion`)

**Prompt (Midjourney v6.1):**
```
Isometric 3D illustration of a friendly robot assistant holding a glowing document 
with a green checkmark floating above it. The robot has soft, rounded features with 
an indigo (#6366f1) and violet (#8b5cf6) color scheme. A calculator and spreadsheet 
float nearby. Soft ambient lighting, no harsh shadows, white/transparent background. 
Clean, modern SaaS product illustration style. Rounded edges on all objects. 
--ar 4:3 --style raw --no text, watermark, realistic
```

**Negative Prompts:** text, watermark, realistic human, dark background, harsh shadows, complex patterns

---

### 2.2 Dashboard Empty State

**Filename:** `empty-state-illustration.png`  
**Dimensions:** 400×300px → WebP (<50KB)  
**In-App Location:** Dashboard when no calculations have been run

**Prompt (Midjourney v6.1):**
```
Isometric 3D illustration of a friendly small robot sitting on a stack of blank 
documents, looking up curiously with a magnifying glass. Soft indigo (#6366f1) and 
teal (#14b8a6) color palette. Clean white background, no shadows, minimal style. 
A thought bubble with "?" floats nearby. Modern SaaS empty state illustration. 
--ar 4:3 --style raw --no text, watermark, realistic
```

---

### 2.3 Error State Illustration

**Filename:** `error-state-illustration.png`  
**Dimensions:** 300×250px → WebP (<40KB)  
**In-App Location:** Error pages, failed upload states

**Prompt (DALL·E 3):**
```
A soft 3D isometric illustration of a small robot looking confused next to a broken 
document with a red warning triangle. Color palette: indigo and rose (#f43f5e). 
Clean white background, rounded forms, modern SaaS style. No text.
```

---

## 3. GST Rule Icons

### Style Guide for All Rule Icons
- **Dimensions:** 128×128px → WebP or **GIF** (<20KB each)
- **Style:** Soft 3D isometric, single subject, transparent/white background
- **Color Palette:** Each icon uses Indigo base + one accent color
- **GIF Option:** For "live" rules, consider making a subtle 2-3 second looping GIF (e.g., the clock hand ticking on Rule 37, the matching lines connecting on Rule 36(4)) — adds life without heavy Lottie dependency
- **In-App Location:** Landing page GST Rules section

### 3.1 Rule 37 — 180-Day ITC Reversal

**Filename:** `rule-37.png`  
**Prompt (Midjourney v6.1):**
```
Isometric 3D icon of a calendar showing "180" with a clock overlay and a circular 
arrow indicating reversal. Indigo (#6366f1) and amber (#f59e0b) accents. Soft 
rounded edges, clean white background, modern fintech icon style. 
--ar 1:1 --style raw --no text, watermark
```

### 3.2 Rule 36(4) — ITC Matching

**Filename:** `rule-36-4.png`  
**Prompt (Midjourney v6.1):**
```
Isometric 3D icon of two documents side by side with connecting dotted lines showing 
a matching/comparison process. One document has a green checkmark, another has a 
yellow question mark. Indigo (#6366f1) and teal (#14b8a6) palette. Clean white 
background, rounded soft 3D style. 
--ar 1:1 --style raw --no text
```

### 3.3 Rule 86B — Credit Restriction

**Filename:** `rule-86b.png`  
**Prompt (Midjourney v6.1):**
```
Isometric 3D icon of a shield with a percentage meter at 99% and a lock symbol. 
Indigo (#6366f1) and rose (#f43f5e) color accents. Rounded 3D forms, clean white 
background, modern SaaS icon. 
--ar 1:1 --style raw --no text
```

### 3.4 Rule 16(4) — ITC Time Limit

**Filename:** `rule-16-4.png`  
**Prompt (Midjourney v6.1):**
```
Isometric 3D icon of an hourglass with a document and clock showing a deadline. 
Sand inside is indigo (#6366f1) colored. Amber (#f59e0b) accent for urgency. 
Rounded soft edges, clean white background, fintech icon style. 
--ar 1:1 --style raw --no text
```

### 3.5 GSTR-3B Compliance

**Filename:** `gstr-3b.png`  
**Prompt (Midjourney v6.1):**
```
Isometric 3D icon of a filing form/tax return document with a green checkmark badge 
and the Indian rupee (₹) symbol. Indigo and teal green palette. Clean white 
background, rounded soft 3D style.
--ar 1:1 --style raw --no text
```

### 3.6 GSTR-9 Annual Return

**Filename:** `gstr-9.png`  
**Prompt (Midjourney v6.1):**
```
Isometric 3D icon of an annual report book with a bar chart and year "2025" on the 
cover. Indigo (#6366f1) and violet (#8b5cf6) gradient. Rounded 3D forms, white 
background. 
--ar 1:1 --style raw --no text
```

---

## 4. Feature Icons

### Style Guide
- **Dimensions:** 96×96px → WebP (<15KB each)
- **Style:** Consistent with rule icons but slightly smaller/simpler
- **In-App Location:** Landing page bento feature grid

### 4.1 Instant Calculation (Bolt)

**Filename:** `smart-calc.png`  
**Prompt (DALL·E 3):**
```
A soft 3D isometric icon of a lightning bolt striking a calculator, with small 
sparkle particles around it. Indigo (#6366f1) and yellow accent. Clean white 
background, rounded edges, SaaS product icon. No text.
```

### 4.2 Peace of Mind Dashboard

**Filename:** `peace-of-mind.png`  
**Prompt (DALL·E 3):**
```
A soft 3D isometric icon of a dashboard screen showing a large green checkmark 
with a small happy face emoji. Teal (#14b8a6) and white palette. Clean background, 
rounded edges, modern app icon. No text.
```

### 4.3 Bank-Grade Security

**Filename:** `security.png`  
**Prompt (DALL·E 3):**
```
A soft 3D isometric icon of a shield with a lock symbol and a cloud inside. 
Indigo blue (#6366f1) shield with gold accent lock. Clean white background, 
rounded soft edges. No text.
```

### 4.4 Always Updated (Auto-Sync)

**Filename:** `auto-sync.png`  
**Prompt (DALL·E 3):**
```
A soft 3D isometric icon of two circular arrows forming a sync loop around a 
gear/cog. Indigo (#6366f1) and violet (#8b5cf6) gradient. Clean white background, 
rounded 3D style. No text.
```

### 4.5 Report Export

**Filename:** `reports.png`  
**Prompt (DALL·E 3):**
```
A soft 3D isometric icon of an Excel spreadsheet with a download arrow. Green 
accent for the Excel badge. Indigo (#6366f1) document. Clean white background, 
rounded edges. No text.
```

---

## 5. Step Illustrations

### Style Guide
- **Dimensions:** 200×200px → WebP (<30KB each)
- **Style:** Slightly more detailed isometric scenes (not just single objects)
- **In-App Location:** Landing page "How it Works" timeline

### 5.1 Step 1 — Export Ledger

**Filename:** `step-1-export.png`  
**Prompt (Midjourney v6.1):**
```
Isometric 3D scene: A laptop showing Tally software with an Excel file flying out 
from the screen into a floating folder. Soft indigo and green accents. Clean white 
background, rounded 3D forms, modern SaaS illustration. 
--ar 1:1 --style raw --no text
```

### 5.2 Step 2 — Upload & Go

**Filename:** `step-2-upload.png`  
**Prompt (Midjourney v6.1):**
```
Isometric 3D scene: A hand dragging an Excel file towards a cloud upload portal 
with an upward arrow. Small sparkle particles around the upload area. Indigo and 
violet palette. Clean white background, rounded edges. 
--ar 1:1 --style raw --no text
```

### 5.3 Step 3 — Get Results

**Filename:** `step-3-results.png`  
**Prompt (Midjourney v6.1):**
```
Isometric 3D scene: A floating dashboard card showing "All Clear ✓" with a green 
checkmark, alongside a small pie chart and bar graph. Teal and indigo palette. 
Clean white background, celebration confetti particles nearby.
--ar 1:1 --style raw --no text
```

---

## 6. Auth Screen Illustrations (Split-Panel)

### Style Guide
- **Dimensions:** 400×400px (login/signup), 300×300px (verify/reset) → WebP (<60KB each)
- **Style:** Consistent with hero illustration — same 3D isometric, same lighting, same character
- **In-App Location:** Auth page left brand panel

### 6.1 Login — Robot Waving

**Filename:** `auth-login-illustration.png`  
**Output Path:** `frontend/src/assets/images/auth/auth-login-illustration.png`
**Prompt (Midjourney v6.1):**
```
Isometric 3D illustration of a friendly robot assistant waving hello with one hand, 
standing next to a shield with a green checkmark on it. The robot has soft rounded 
features with indigo (#6366f1) and violet (#8b5cf6) color scheme. A small 
"Welcome" speech bubble floats above. Soft ambient lighting, transparent/white 
background, modern SaaS product illustration. 
--ar 1:1 --style raw --no text, watermark, realistic, dark background
```

### 6.2 Signup — Robot Holding Checkmark

**Filename:** `auth-signup-illustration.png`  
**Output Path:** `frontend/src/assets/images/auth/auth-signup-illustration.png`
**Prompt (Midjourney v6.1):**
```
Isometric 3D illustration of a friendly robot assistant celebrating while holding a 
large green checkmark above its head. Small confetti particles float around. 
A blank user profile card floats nearby with a "+" symbol. Indigo (#6366f1) and 
teal (#14b8a6) palette. Transparent/white background, rounded soft 3D forms. 
--ar 1:1 --style raw --no text, watermark, realistic
```

### 6.3 Verify Email — Envelope with Sparkles

**Filename:** `auth-verify-illustration.png`  
**Output Path:** `frontend/src/assets/images/auth/auth-verify-illustration.png`
**Prompt (Midjourney v6.1):**
```
Isometric 3D illustration of a floating open envelope with a glowing letter coming 
out, surrounded by golden sparkle particles. A small "6-digit code" badge floats 
nearby. Indigo (#6366f1) envelope with teal (#14b8a6) sparkle accents. Clean white 
background, soft ambient lighting. 
--ar 1:1 --style raw --no text, watermark, realistic
```

### 6.4 Password Reset — Lock with Key

**Filename:** `auth-reset-illustration.png`  
**Output Path:** `frontend/src/assets/images/auth/auth-reset-illustration.png`
**Prompt (Midjourney v6.1):**
```
Isometric 3D illustration of a friendly lock character with a floating golden key 
approaching it. Small shield icons float nearby for security. Indigo (#6366f1) lock 
with gold (#f59e0b) key accents. Clean white background, rounded soft 3D forms, 
modern SaaS style. 
--ar 1:1 --style raw --no text, watermark, realistic
```

### 6.5 Auth Background Dot Pattern

**Filename:** `auth-bg-pattern.svg`  
**Type:** Hand-crafted SVG (not AI-generated)
```svg
<svg width="20" height="20" xmlns="http://www.w3.org/2000/svg">
  <circle cx="10" cy="10" r="1" fill="rgba(99,102,241,0.08)"/>
</svg>
```
*Usage:* `background-image: url('auth-bg-pattern.svg'); background-size: 20px 20px;` on the brand panel.

---

## 7. UI Backgrounds & Textures

### 6.1 Landing Hero Gradient Mesh

**Usage:** Background behind hero section (CSS fallback if image fails)  
**Recommended:** CSS-only (no image needed)

```scss
.gradient-mesh-bg {
    background: 
        radial-gradient(at 20% 30%, rgba(99, 102, 241, 0.15) 0%, transparent 50%),
        radial-gradient(at 80% 20%, rgba(168, 85, 247, 0.12) 0%, transparent 50%),
        radial-gradient(at 50% 80%, rgba(14, 165, 233, 0.08) 0%, transparent 50%);
}
```

### 6.2 Pricing Section Dark Gradient

**Filename:** `pricing-bg.webp`  
**Dimensions:** 1920×1080 → WebP (<80KB)  
**In-App Location:** Pricing section background

**Prompt (Midjourney v6.1):**
```
Abstract gradient mesh background in deep navy (#0f172a) with subtle indigo 
(#6366f1) and violet (#8b5cf6) glowing orbs. Very minimal, dark, moody. Grain 
noise texture overlaid. Ultra-wide aspect ratio. 
--ar 16:9 --style raw --no objects, text, shapes
```

### 6.3 Glass Noise Texture Overlay

**Filename:** `noise-texture.png`  
**Dimensions:** 200×200px, tileable → PNG (<5KB)  
**Usage:** CSS `background-image` overlay on glass cards for frosted effect

**Prompt (DALL·E 3):**
```
A seamless tileable noise/grain texture in very light gray. Subtle, barely visible 
static noise pattern. Clean, minimal. Transparent background with white noise dots 
at 5% opacity.
```

> [!TIP]
> This texture can also be generated programmatically with a 10-line JavaScript canvas script — often better than AI generation for pure noise.

---

## 8. Lottie / GIF Animations

### 7.1 Document Processing Loader

**Filename:** `processing-loader.json` (Lottie)  
**Dimensions:** 200×200px  
**Duration:** 2s loop  
**In-App Location:** Dashboard, during ledger analysis processing state

**LottieFiles AI Prompt:**
```
A document/paper with pages flipping, being scanned by a blue light beam. 
Small particle dots appear as data is extracted. Color: indigo blue (#6366f1). 
Smooth loop animation, 24fps, minimal style.
```

**Frame-by-Frame Description:**
1. **Frame 0-12:** Document floats in center, gentle bob animation
2. **Frame 12-36:** Blue scan line sweeps top→bottom across document
3. **Frame 36-48:** Small data dots (particles) fly out from document edges
4. **Frame 48-60:** Particles converge into a checkmark shape, then reset to frame 0

---

### 7.2 Success Celebration

**Filename:** `success-confetti.json` (Lottie)  
**Dimensions:** 300×300px  
**Duration:** 1.5s (plays once)  
**In-App Location:** After successful calculation, verdict banner "All Clear"

**LottieFiles AI Prompt:**
```
Green and teal confetti burst from center, floating outward with gravity. 
Small circles, squares, and triangles in green (#14b8a6) and gold (#f59e0b). 
Plays once, not looping. Light and celebratory.
```

---

### 7.3 Empty State Float

**Filename:** `empty-float.json` (Lottie)  
**Dimensions:** 200×150px  
**Duration:** 3s loop  
**In-App Location:** Dashboard history tab when no saved calculations exist

**LottieFiles AI Prompt:**
```
A small robot character sitting on a cloud, gently floating up and down. 
Indigo (#6366f1) robot with teal (#14b8a6) accents. Small question mark 
bubble appears and fades. Smooth ease-in-out floating loop.
```

---

### 7.4 Upload Dropzone Active State

**Filename:** `upload-active.json` (Lottie)  
**Dimensions:** 100×100px  
**Duration:** 1s loop  
**In-App Location:** Document upload area when user hovers/drags file

**LottieFiles AI Prompt:**
```
An upward arrow inside a dashed circle, pulsing gently. The arrow bounces 
upward slightly on each pulse. Indigo blue (#6366f1) color. Minimal, clean, 
smooth loop.
```

---

### 7.5 Credit Deduction Micro-Animation

**Filename:** `credit-deduct.json` (Lottie)  
**Dimensions:** 40×40px  
**Duration:** 0.5s (plays once)  
**In-App Location:** Credit wallet badge when a credit is consumed

**LottieFiles AI Prompt:**
```
A coin with "C" on it flies upward, shrinks, and disappears with a small 
sparkle. Violet (#8b5cf6) coin. Very fast, 12 frames. Minimal SaaS style.
```

---

## 9. Product & Demo Videos

### 8.1 Landing Page Hero Video (Background Loop)

**Platform:** Sora  
**Duration:** 8 seconds, loop  
**Dimensions:** 1920×1080  
**Format:** WebM (<2MB) + MP4 fallback  
**In-App Location:** Optional: hero section background video (behind particles)

**Sora Prompt:**
```
Slow, cinematic camera movement over an abstract isometric landscape of floating 
glass cards, documents, and data visualizations. Soft indigo and violet ambient 
lighting. Shallow depth of field. Modern, clean, technology aesthetic. No people. 
No text. Dreamy and premium feel. Loopable. 8 seconds.
```

---

### 8.2 Product Walkthrough Demo (90 seconds)

**Platform:** Kling 1.6 (scene-by-scene)  
**Duration:** 90 seconds  
**Format:** MP4 (1080p)  
**In-App Location:** Landing page video modal

**Storyboard:**

| Shot | Duration | Camera | Subject | Mood |
|---|---|---|---|---|
| 1. Intro | 5s | Static, centered | GST Buddy logo animates in with particle effect | Premium, minimal |
| 2. Problem | 15s | Slow zoom in | Overwhelmed accountant at desk with piles of Excel sheets. Text overlay: "Tracking GST compliance manually?" | Empathetic, relatable |
| 3. Solution | 10s | Pan right | Screen recording: user opens GST Buddy dashboard, clean glass UI | Relieving, modern |
| 4. Upload | 15s | Close-up on screen | Drag-and-drop upload of Excel file. Processing animation plays. | Fast, capable |
| 5. Results | 15s | Slight zoom out | Dashboard shows "All Clear ✓". Confetti animation. ITC numbers displayed. | Triumphant, satisfying |
| 6. Export | 10s | Close-up | Click "Export to Excel" button. Download confirmation. | Productive, efficient |
| 7. Pricing | 10s | Static | Pricing cards appear. "Try Free" highlighted. | Fair, accessible |
| 8. CTA | 10s | Slow zoom in on CTA | "Start Free" button with glow. Text: "5 free compliance checks" | Motivating, low-friction |

**Kling Prompt (Shot 2 example):**
```
A close-up shot of an Indian accountant sitting at a desk covered with Excel 
spreadsheets and tax forms. The person looks slightly overwhelmed but not 
distressed. Warm office lighting, shallow depth of field. Modern Indian office 
setting. Camera slowly zooms in. Duration: 5 seconds.
```

---

## 10. Avatars & Testimonials

### Style Guide
- **Dimensions:** 80×80px → WebP (<10KB each)
- **Style:** Illustrated portraits (not photographic), consistent art style
- **In-App Location:** Landing page testimonials section

### 9.1 Rajesh Kumar (CA Partner, Mumbai)

**Filename:** `avatar-1.png`  
**Prompt (DALL·E 3):**
```
A friendly illustrated portrait avatar of a middle-aged Indian man with glasses 
and a warm smile. Professional appearance, wearing a formal shirt. Soft, clean 
illustration style with subtle indigo tint. Circular crop, white background. 
No text.
```

### 9.2 Priya Sharma (Finance Head, Surat)

**Filename:** `avatar-2.png`  
**Prompt (DALL·E 3):**
```
A friendly illustrated portrait avatar of an Indian woman in her 30s with a 
confident smile. Professional appearance, wearing business attire. Soft, clean 
illustration style with subtle teal tint. Circular crop, white background. 
No text.
```

### 9.3 Amit Patel (Business Owner, Ahmedabad)

**Filename:** `avatar-3.png`  
**Prompt (DALL·E 3):**
```
A friendly illustrated portrait avatar of a young Indian man with a casual smile. 
Business casual appearance. Soft, clean illustration style with subtle violet tint. 
Circular crop, white background. No text.
```

---

## Usage Notes Summary

| Asset | File | Location in App | Priority |
|---|---|---|---|
| Hero illustration | `hero-illustration.png` | Landing hero right side | 🔴 P0 |
| Rule 37 icon | `rule-37.png` | Landing rules section | 🟡 P1 |
| Rule 36(4) icon | `rule-36-4.png` | Landing rules section | 🟡 P1 |
| Rule 86B icon | `rule-86b.png` | Landing rules section | 🟡 P1 |
| Rule 16(4) icon | `rule-16-4.png` | Landing rules section | 🟡 P1 |
| GSTR-3B icon | `gstr-3b.png` | Landing rules section | 🟡 P1 |
| GSTR-9 icon | `gstr-9.png` | Landing rules section | 🟡 P1 |
| Smart calc icon | `smart-calc.png` | Landing features bento | 🟡 P1 |
| Peace of mind icon | `peace-of-mind.png` | Landing features bento | 🟡 P1 |
| Security icon | `security.png` | Landing features bento | 🟡 P1 |
| Auto-sync icon | `auto-sync.png` | Landing features bento | 🟡 P1 |
| Reports icon | `reports.png` | Landing features bento | 🟢 P2 |
| Step 1 illustration | `step-1-export.png` | Landing timeline | 🟡 P1 |
| Step 2 illustration | `step-2-upload.png` | Landing timeline | 🟡 P1 |
| Step 3 illustration | `step-3-results.png` | Landing timeline | 🟡 P1 |
| Auth login illustration | `auth-login-illustration.png` | Auth login left panel | ✅ Done |
| Auth signup illustration | `auth-signup-illustration.png` | Auth signup left panel | ✅ Done |
| Auth verify illustration | `auth-verify-illustration.png` | Auth verify-email left panel | ✅ Done |
| Auth reset illustration | `auth-reset-illustration.png` | Auth pwd-reset left panel | ✅ Done |
| Auth bg pattern | `auth-bg-pattern.svg` | Auth brand panel background | 🟢 P2 |
| Empty state | `empty-state-illustration.png` | Dashboard empty | 🟡 P1 |
| Error state | `error-state-illustration.png` | Error pages | 🟢 P2 |
| Processing loader | `processing-loader.json` | Dashboard processing | 🔴 P0 |
| Success confetti | `success-confetti.json` | Dashboard verdict | 🟡 P1 |
| Empty float | `empty-float.json` | Dashboard history empty | 🟢 P2 |
| Upload active | `upload-active.json` | Document upload hover | 🟢 P2 |
| Credit deduction | `credit-deduct.json` | Credit wallet badge | 🟢 P2 |
| Pricing BG | `pricing-bg.webp` | Landing pricing section | 🟢 P2 |
| Noise texture | `noise-texture.png` | Glass card overlay | 🟢 P2 |
| Avatar 1 | `avatar-1.png` | Landing testimonials | 🟢 P2 |
| Avatar 2 | `avatar-2.png` | Landing testimonials | 🟢 P2 |
| Avatar 3 | `avatar-3.png` | Landing testimonials | 🟢 P2 |
| Hero video loop | Background WebM | Landing hero (optional) | 🟢 P2 |
| Product demo | MP4 walkthrough | Landing video modal | 🟢 P2 |
