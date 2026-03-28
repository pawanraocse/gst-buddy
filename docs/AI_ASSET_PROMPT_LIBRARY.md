# AI Asset Generation Prompt Library

**Project:** GSTbuddies  
**Version:** 1.0  
**Date:** Feb 2026  
**Visual Direction:** Professional Financial Precision — High-fidelity 3D Glassmorphism, Indigo/Violet & Silver palette, ultra-realistic textures, clean architectural studio lighting.

---

## Table of Contents

1. [Model Recommendations](#1-model-recommendations)
2. [Hero Illustrations](#2-hero-illustrations)
3. [GST Rule Icons (128×128)](#3-gst-rule-icons)
4. [Feature Icons (96×96)](#4-feature-icons)
5. [Step Illustrations (200×200)](#5-step-illustrations)
6. [Auth Screen Illustrations](#6-auth-screen-illustrations-split-panel)
7. [UI Backgrounds & Textures](#7-ui-backgrounds--textures)
8. [Lottie Micro-Animations](#8-lottie-micro-animations)
9. [Product & Demo Videos](#9-product--demo-videos)
10. [Avatars & Testimonials](#10-avatars--testimonials)
11. [Interactive Impact Video](#11-interactive-impact-video-marketingdemo)

---

## 1. Model Recommendations

| Asset Type | Recommended Tool | Format Strategy |
|---|---|---|
| **3D Isometric Illustrations** | **Leonardo.ai** or **Microsoft Designer** | **WebP** (80% quality) — Best compression for complex 3D refractions |
| **Icons (Rule/Feature)** | **Microsoft Designer** | **WebP** for 3D, or trace to **SVG** for flat/line art |
| **Animated Icons** | **LottieFiles AI** | **Lottie JSON** (<20KB) — Avoid GIF entirely for small loop animations |
| **Avatars** | **Microsoft Designer** | **WebP** — Best for photorealistic portraits |
| **Product Videos** | **Luma Dream Machine** or **Runway** | **MP4 / WebM** (WebM for transparent backgrounds if needed) |

### 1.1 Format Strategy for Performance
To maintain "High-fidelity 3D Glassmorphism" while keeping build sizes minimal:
1. **Never use PNG or JPEG.** Always export 3D raster assets to **WebP (Lossy, 80-85% quality)**. This reduces size by 40-70% with no visible quality loss.
2. **Retina (2x) Sizing:** Generate icons at 2x their display size to ensure crispness on high-DPI screens (e.g., if displaying at 64x64, generate at 128x128).
3. **Animations:** Rely strictly on **Lottie (JSON)** for all UI micro-animations and loaders. They act computationally like SVGs and are incredibly small (<20KB) compared to GIFs (>500KB). Do not use GIFs.
4. **SVG SVGO Optimization:** For any hand-crafted vector backgrounds (like dot patterns), run them through an SVGO optimizer to strip metadata.

---

## 2. Hero Illustrations

### 2.1 Landing Page Hero — Main Illustration

**Filename:** `hero-illustration.png`  
**Dimensions:** 800×600px → Export as WebP (<100KB)  
**In-App Location:** Landing page, hero section right side (`div.ai-companion`)
**Output Path:** `frontend/src/assets/images/hero/hero-illustration.png`

**Prompt (Leonardo.ai - Phoenix):**
```
Ultra-realistic 3D close-up of a high-end translucent glass dashboard displaying glowing GST compliance scores. A brilliant green verification checkmark hovers above holographic data visualizations. The aesthetic is 'Professional Financial Precision' with an indigo (#6366f1) and violet (#8b5cf6) color scheme, accented by brushed silver. Cinematic studio lighting, sharp focus, clean white/transparent background. Premium B2B SaaS aesthetics.
```

**Negative Prompts:** text, watermark, realistic human, dark background, harsh shadows, complex patterns

---

### 2.2 Dashboard Empty State

**Filename:** `empty-state-illustration.png`  
**Output Path:** `frontend/src/assets/images/dashboard/empty-state-illustration.png`
**Dimensions:** 400×300px → WebP (<50KB)  
**In-App Location:** Dashboard when no calculations have been run

**Prompt (Leonardo.ai - Phoenix):**
```
High-fidelity 3D illustration of a neat stack of translucent frosted glass folders. Floating slightly above them is a beautifully rendered golden holographic 'Search' magnifying glass icon. Soft indigo (#6366f1) and teal (#14b8a6) lighting refracts through the glass. Clean white isolated background, premium corporate SaaS empty state illustration, hyper-detailed.
```

---

### 2.3 Error State Illustration

**Filename:** `error-state-illustration.png`  
**Output Path:** `frontend/src/assets/images/dashboard/error-state-illustration.png`
**Dimensions:** 300×250px → WebP (<40KB)  
**In-App Location:** Error pages, failed upload states

**Prompt (Microsoft Designer):**
```
A minimalist, high-fidelity 3D translucent warning triangle made of frosted glass, glowing with a soft ruby red (#f43f5e) inner light. The sign is suspended inside a subtle glass cube. Color palette: indigo and rose. Clean white background, sleek corporate design language, modern SaaS error state illustration. No text.
```

---

## 3. GST Rule Icons

### Style Guide for All Rule Icons
- **Dimensions:** Generate at 256×256px → Export as **WebP** (<15KB) → Display at 128×128px.
- **Style:** Soft 3D isometric, single subject, transparent/white background.
- **Color Palette:** Each icon uses Indigo base + one accent color.
- **Animation Option:** If animation is desired, convert static design into a **Lottie JSON** using AfterEffects/LottieFiles (e.g., ticking clock hand). *Do not use GIF format.*
- **In-App Location:** Landing page GST Rules section.

### 3.1 Rule 37 — 180-Day ITC Reversal

**Filename:** `rule-37.png`  
**Output Path:** `frontend/src/assets/images/landing/icons/rule-37.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Isometric 3D icon of a calendar showing "180" with a clock overlay and a circular 
arrow indicating reversal. Indigo (#6366f1) and amber (#f59e0b) accents. Soft 
rounded edges, clean white background, modern fintech icon style.
```

### 3.2 Rule 36(4) — ITC Matching

**Filename:** `rule-36-4.png`  
**Output Path:** `frontend/src/assets/images/landing/icons/rule-36-4.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Isometric 3D icon of two documents side by side with connecting dotted lines showing 
a matching/comparison process. One document has a green checkmark, another has a 
yellow question mark. Indigo (#6366f1) and teal (#14b8a6) palette. Clean white 
background, rounded soft 3D style.
```

### 3.3 Rule 86B — Credit Restriction

**Filename:** `rule-86b.png`  
**Output Path:** `frontend/src/assets/images/landing/icons/rule-86b.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Isometric 3D icon of a shield with a percentage meter at 99% and a lock symbol. 
Indigo (#6366f1) and rose (#f43f5e) color accents. Rounded 3D forms, clean white 
background, modern SaaS icon.
```

### 3.4 Rule 16(4) — ITC Time Limit

**Filename:** `rule-16-4.png`  
**Output Path:** `frontend/src/assets/images/landing/icons/rule-16-4.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Isometric 3D icon of an hourglass with a document and clock showing a deadline. 
Sand inside is indigo (#6366f1) colored. Amber (#f59e0b) accent for urgency. 
Rounded soft edges, clean white background, fintech icon style.
```

### 3.5 GSTR-3B Compliance

**Filename:** `gstr-3b.png`  
**Output Path:** `frontend/src/assets/images/landing/icons/gstr-3b.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Isometric 3D icon of a filing form/tax return document with a green checkmark badge 
and the Indian rupee (₹) symbol. Indigo and teal green palette. Clean white 
background, rounded soft 3D style.
```

### 3.6 GSTR-9 Annual Return

**Filename:** `gstr-9.png`  
**Output Path:** `frontend/src/assets/images/landing/icons/gstr-9.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Isometric 3D icon of an annual report book with a bar chart and year "2025" on the 
cover. Indigo (#6366f1) and violet (#8b5cf6) gradient. Rounded 3D forms, white 
background.
```

---

## 4. Feature Icons

### Style Guide
- **Dimensions:** Generate at 128×128px → Export as **WebP** (<10KB) → Display at 64×64px.
- **Style:** Consistent with rule icons but slightly smaller/simpler. (Alternative: If you switch these to flat 2D designs, use **SVG** <2KB).
- **In-App Location:** Landing page bento feature grid.

### 4.1 Instant Calculation (Bolt)

**Filename:** `smart-calc.png`  
**Output Path:** `frontend/src/assets/images/landing/features/smart-calc.png`
**Prompt (Microsoft Designer):**
```
A soft 3D isometric icon of a lightning bolt striking a calculator, with small 
sparkle particles around it. Indigo (#6366f1) and yellow accent. Clean white 
background, rounded edges, SaaS product icon. No text.
```

### 4.2 Peace of Mind Dashboard

**Filename:** `peace-of-mind.png`  
**Output Path:** `frontend/src/assets/images/landing/features/peace-of-mind.png`
**Prompt (Microsoft Designer):**
```
A soft 3D isometric icon of a dashboard screen showing a large green checkmark 
with a small happy face emoji. Teal (#14b8a6) and white palette. Clean background, 
rounded edges, modern app icon. No text.
```

### 4.3 Bank-Grade Security

**Filename:** `security.png`  
**Output Path:** `frontend/src/assets/images/landing/features/security.png`
**Prompt (Microsoft Designer):**
```
A soft 3D isometric icon of a shield with a lock symbol and a cloud inside. 
Indigo blue (#6366f1) shield with gold accent lock. Clean white background, 
rounded soft edges. No text.
```

### 4.4 Always Updated (Auto-Sync)

**Filename:** `auto-sync.png`  
**Output Path:** `frontend/src/assets/images/landing/features/auto-sync.png`
**Prompt (Microsoft Designer):**
```
A soft 3D isometric icon of two circular arrows forming a sync loop around a 
gear/cog. Indigo (#6366f1) and violet (#8b5cf6) gradient. Clean white background, 
rounded 3D style. No text.
```

### 4.5 Report Export

**Filename:** `reports.png`  
**Output Path:** `frontend/src/assets/images/landing/features/reports.png`
**Prompt (Microsoft Designer):**
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
**Output Path:** `frontend/src/assets/images/steps/step-1-export.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Photorealistic top-down view of a sleek, high-end silver laptop on a minimalist white desk. The laptop screen displays a pristine financial spreadsheet. A glowing 3D glass folder icon floats slightly above the keyboard. Soft, diffused morning sunlight streaming across the desk. Premium corporate financial software aesthetic, indigo color accents.
```

### 5.2 Step 2 — Upload & Go

**Filename:** `step-2-upload.png`  
**Output Path:** `frontend/src/assets/images/steps/step-2-upload.png`
**Prompt (Leonardo.ai - Phoenix):**
```
High-fidelity 3D illustration representing a cloud upload. A beautifully rendered translucent glass document with an Excel logo is being pulled upward by a sleek, glowing indigo (#6366f1) arrow into an abstract frosted glass cloud shape. Soft violet lighting, premium corporate SaaS design, clean white background.
```

### 5.3 Step 3 — Get Results

**Filename:** `step-3-results.png`  
**Output Path:** `frontend/src/assets/images/steps/step-3-results.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Professional 3D rendering of a modern financial dashboard interface made of tiered frosted glass layers. The prominent central card features a brilliant metallic green checkmark signifying 'All Clear'. Subtle elegant holographic pie charts and bar graphs glow in teal and indigo around it. Clean white background, hyper-detailed UI elements.
```

---

## 6. Auth Screen Illustrations (Split-Panel)

### Style Guide
- **Dimensions:** 400×400px (login/signup), 300×300px (verify/reset) → WebP (<60KB each)
- **Style:** Photorealistic, cinematic lighting, modern corporate office environments
- **In-App Location:** Auth page left brand panel

### 6.1 Login — Robot Waving

**Filename:** `auth-login-illustration.png`  
**Output Path:** `frontend/src/assets/images/auth/auth-login-illustration.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Highly realistic, cinematic medium shot of a confident Indian male Chartered Accountant (30s) in a modern, glass-walled office boardroom. He is looking at the camera with a subtle, trustworthy smile. He wears a sharp navy-blue suit. Soft morning sunlight filters through the window. Shallow depth of field, premium corporate photography style, indigo color grading.
```

### 6.2 Signup — Robot Holding Checkmark

**Filename:** `auth-signup-illustration.png`  
**Output Path:** `frontend/src/assets/images/auth/auth-signup-illustration.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Highly realistic, cinematic medium shot of an Indian female Finance Director (30s) standing in a high-tech modern office space. She is smiling confidently, holding a sleek silver tablet. Soft, elegant lighting with subtle teal and indigo ambient glows in the background. Shallow depth of field, premium corporate photography style.
```

### 6.3 Verify Email — Envelope with Sparkles

**Filename:** `auth-verify-illustration.png`  
**Output Path:** `frontend/src/assets/images/auth/auth-verify-illustration.png`
**Prompt (Leonardo.ai - Phoenix):**
```
Close-up macro photography of a person's hand holding a sleek, modern smartphone displaying a glowing blue 'Verification Code' notification. The background is a beautifully blurred modern office environment (bokeh effect). Indigo (#6366f1) lighting accents. Hyper-realistic, 8k resolution, cinematic lighting.
```

### 6.4 Password Reset — Lock with Key

**Filename:** `auth-reset-illustration.png`  
**Output Path:** `frontend/src/assets/images/auth/auth-reset-illustration.png`
**Prompt (Leonardo.ai - Phoenix):**
```
High-fidelity 3D rendering of a sophisticated biometric fingerprint lock made of brushed steel and glowing indigo glass. The lock sits on a clean, dark slate surface. Premium, secure, high-tech fintech aesthetic. Dramatic studio lighting, shallow depth of field.
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
**Output Path:** `frontend/src/assets/images/landing/pricing-bg.webp`
**Dimensions:** 1920×1080 → WebP (<80KB)  
**In-App Location:** Pricing section background

**Prompt (Leonardo.ai - Phoenix):**
```
Abstract gradient mesh background in deep navy (#0f172a) with subtle indigo 
(#6366f1) and violet (#8b5cf6) glowing orbs. Very minimal, dark, moody. Grain 
noise texture overlaid. Ultra-wide aspect ratio.
```

### 6.3 Glass Noise Texture Overlay

**Filename:** `noise-texture.png`  
**Dimensions:** 200×200px, tileable → PNG (<5KB)  
**Usage:** CSS `background-image` overlay on glass cards for frosted effect

**Prompt (Microsoft Designer):**
```
A seamless tileable noise/grain texture in very light gray. Subtle, barely visible 
static noise pattern. Clean, minimal. Transparent background with white noise dots 
at 5% opacity.
```

> [!TIP]
> This texture can also be generated programmatically with a 10-line JavaScript canvas script — often better than AI generation for pure noise.

---

## 8. Lottie Micro-Animations

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
A sleek translucent glass folder gently floating up and down. A golden holographic 'Search' magnifying glass icon appears and fades above it. Indigo (#6366f1) and teal (#14b8a6) lighting accents. Smooth ease-in-out floating loop. Premium corporate design.
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

**Platform:** Luma Dream Machine (Free Trial)  
**Duration:** 8 seconds, loop  
**Dimensions:** 1920×1080  
**Format:** WebM (<2MB) + MP4 fallback  
**In-App Location:** Optional: hero section background video (behind particles)

**Luma Prompt:**
```
Slow, cinematic camera movement over an abstract isometric landscape of floating 
glass cards, documents, and data visualizations. Soft indigo and violet ambient 
lighting. Shallow depth of field. Modern, clean, technology aesthetic. No people. 
No text. Dreamy and premium feel. Loopable. 8 seconds.
```

---

### 8.2 Product Walkthrough Demo (90 seconds)

**Platform:** Runway Gen-3 Alpha (Free Trial) (scene-by-scene)  
**Duration:** 90 seconds  
**Format:** MP4 (1080p)  
**In-App Location:** Landing page video modal

**Storyboard:**

| Shot | Duration | Camera | Subject | Mood |
|---|---|---|---|---|
| 1. Intro | 5s | Static, centered | GSTbuddies logo animates in with particle effect | Premium, minimal |
| 2. Problem | 15s | Slow zoom in | Overwhelmed accountant at desk with piles of Excel sheets. Text overlay: "Tracking GST compliance manually?" | Empathetic, relatable |
| 3. Solution | 10s | Pan right | Screen recording: user opens GSTbuddies dashboard, clean glass UI | Relieving, modern |
| 4. Upload | 15s | Close-up on screen | Drag-and-drop upload of Excel file. Processing animation plays. | Fast, capable |
| 5. Results | 15s | Slight zoom out | Dashboard shows "All Clear ✓". Confetti animation. ITC numbers displayed. | Triumphant, satisfying |
| 6. Export | 10s | Close-up | Click "Export to Excel" button. Download confirmation. | Productive, efficient |
| 7. Pricing | 10s | Static | Pricing cards appear. "Try Free" highlighted. | Fair, accessible |
| 8. CTA | 10s | Slow zoom in on CTA | "Start Free" button with glow. Text: "5 free compliance checks" | Motivating, low-friction |

**Runway Prompt (Shot 2 example):**
```
Cinematic medium shot of an Indian corporate Finance Manager sitting at a modern glass desk covered with financial reports. He looks slightly stressed, rubbing his forehead. Cool, professional office lighting, shallow depth of field. Modern Indian corporate headquarters setting. Camera slowly pushes in. High-end commercial aesthetic. Duration: 5 seconds.
```

---

## 10. Avatars & Testimonials

### Style Guide
- **Dimensions:** 80×80px → WebP (<10KB each)
- **Style:** Illustrated portraits (not photographic), consistent art style
- **In-App Location:** Landing page testimonials section

### 9.1 Rajesh Kumar (CA Partner, Mumbai)

**Filename:** `avatar-1.png`  
**Output Path:** `frontend/src/assets/images/landing/avatars/avatar-1.png`
**Prompt (Microsoft Designer):**
```
Ultra-realistic studio portrait of a 45-year-old Indian male Chartered Accountant. He is wearing a sharp charcoal suit and glasses, smiling confidently. Corporate boardroom background with soft bokeh. Premium cinematic lighting, highly detailed, photorealistic. Circular crop, 8k resolution.
```

### 9.2 Priya Sharma (Finance Head, Surat)

**Filename:** `avatar-2.png`  
**Output Path:** `frontend/src/assets/images/landing/avatars/avatar-2.png`
**Prompt (Microsoft Designer):**
```
Ultra-realistic studio portrait of a 35-year-old Indian female Finance Director. She is wearing elegant professional business attire, smiling warmly at the camera. Bright, modern glass-walled office background with soft bokeh. Premium cinematic lighting, photorealistic. Circular crop, 8k resolution.
```

### 9.3 Amit Patel (Business Owner, Ahmedabad)

**Filename:** `avatar-3.png`  
**Output Path:** `frontend/src/assets/images/landing/avatars/avatar-3.png`
**Prompt (Microsoft Designer):**
```
Ultra-realistic studio portrait of a 32-year-old Indian male Tech Entrepreneur. He is wearing a professional smart-casual navy blazer over a crisp white shirt, smiling slightly. Minimalist corporate background with soft bokeh. Premium cinematic lighting, photorealistic. Circular crop, 8k resolution.
```

---

## 11. Interactive Impact Video (Marketing/Demo)

### Concept & Objective
Instead of a dry product walkthrough, the landing page video should be an **Interactive Impact Video**. The objective is to motivate the user to sign up by demonstrating the real-world value of GSTbuddies.

### Key Storyboard Elements to Include:
1. **The Problem (0:00 - 0:05):** Show a stressed CA or Business Owner dealing with Excel files, missing deadlines, and worrying about Rule 36(4) mismatches.
2. **The Solution (0:05 - 0:15):** Introduce GSTbuddies. Show a fast-paced, dynamic sequence of a file being uploaded and processed in seconds.
3. **The Impact (0:15 - 0:25):** 
   - **Show the money:** Highlight an "ITC Saved" dashboard metric ticking upwards. 
   - **Show the time:** Visualize dropping from "Days of work" to "Minutes".
   - **Show the people:** Feature smiling, relieved professionals (real people or high-quality avatars) expressing how the tool gave them "Peace of mind" and "Saved ₹50,000 in lost ITC".
4. **Call to Action (0:25 - 0:30):** Direct them to start their free trial.

### Production Style
- **Format:** MP4 or hosted interactive video player (e.g., Vimeo, Wistia)
- **Vibe:** High-energy, professional, empathetic to the user's struggle. Use UI animations mixed with live-action B-roll or expressive 3D avatars.

> [!NOTE]
> *This asset cannot be generated by static AI image tools (like Leonardo / Microsoft Designer). It requires external video production tools (like Adobe After Effects, Canva Video, or a trial video generator like Runway / Luma).*

---

## Usage Notes Summary

| Asset | File | Location in App | Priority |
|---|---|---|---|
| Hero illustration | `hero-illustration.png` | Landing hero right side | ✅ Done |
| Rule 37 icon | `rule-37.png` | Landing rules section | ✅ Done |
| Rule 36(4) icon | `rule-36-4.png` | Landing rules section | ✅ Done |
| Rule 86B icon | `rule-86b.png` | Landing rules section | ✅ Done |
| Rule 16(4) icon | `rule-16-4.png` | Landing rules section | ✅ Done |
| GSTR-3B icon | `gstr-3b.png` | Landing rules section | ✅ Done |
| GSTR-9 icon | `gstr-9.png` | Landing rules section | ✅ Done |
| Smart calc icon | `smart-calc.png` | Landing features bento | ✅ Done |
| Peace of mind icon | `peace-of-mind.png` | Landing features bento | ✅ Done |
| Security icon | `security.png` | Landing features bento | ✅ Done |
| Auto-sync icon | `auto-sync.png` | Landing features bento | ✅ Done |
| Reports icon | `reports.png` | Landing features bento | ✅ Done |
| Step 1 illustration | `step-1-export.png` | Landing timeline | ✅ Done |
| Step 2 illustration | `step-2-upload.png` | Landing timeline | ✅ Done |
| Step 3 illustration | `step-3-results.png` | Landing timeline | ✅ Done |
| Auth login illustration | `auth-login-illustration.png` | Auth login left panel | ✅ Done |
| Auth signup illustration | `auth-signup-illustration.png` | Auth signup left panel | ✅ Done |
| Auth verify illustration | `auth-verify-illustration.png` | Auth verify-email left panel | ✅ Done |
| Auth reset illustration | `auth-reset-illustration.png` | Auth pwd-reset left panel | ✅ Done |
| Auth bg pattern | `auth-bg-pattern.svg` | Auth brand panel background | ✅ Done |
| Empty state | `empty-state-illustration.png` | Dashboard empty | ✅ Done |
| Error state | `error-state-illustration.png` | Error pages | ✅ Done |
| Processing loader | `processing-loader.json` | Dashboard processing | 🔴 Needs Lottie |
| Success confetti | `success-confetti.json` | Dashboard verdict | 🟡 Needs Lottie |
| Empty float | `empty-float.json` | Dashboard history empty | 🟢 Needs Lottie |
| Upload active | `upload-active.json` | Document upload hover | 🟢 Needs Lottie |
| Credit deduction | `credit-deduct.json` | Credit wallet badge | 🟢 Needs Lottie |
| Pricing BG | `pricing-bg.webp` | Landing pricing section | ✅ Done |
| Noise texture | `noise-texture.svg` | Glass card overlay | ✅ Done |
| Avatar 1 | `avatar-1.png` | Landing testimonials | ✅ Done |
| Avatar 2 | `avatar-2.png` | Landing testimonials | ✅ Done |
| Avatar 3 | `avatar-3.png` | Landing testimonials | ✅ Done |
| Hero video loop | Background WebM | Landing hero (optional) | 🟢 Needs AfterEffects |
| Interactive Impact Video | MP4 / YouTube | Landing video modal | 🟢 Needs External Production |
