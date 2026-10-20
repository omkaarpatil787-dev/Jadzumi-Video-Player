# 🎨 Cinema Flow v1.3 — Design Tokens Specification
> **Derived Directly from `stitch_jadzumi_cinema_player_design_system.zip` (`code.html` & `screen.png`)**

---

## 💎 1. Color System & Palettes

All colors match the exact CSS custom properties defined in `code.html`.

### 1.1 Light Theme Palette (`:root`)
| CSS Token | Hex / RGBA | Jetpack Compose `Color` | Usage |
| :--- | :--- | :--- | :--- |
| `--bg-primary` | `#f7faff` | `Color(0xFFF7FAFF)` | Window background, master scaffold |
| `--bg-secondary` | `#eaf4ff` | `Color(0xFFEAF4FF)` | Secondary container surface, search fill |
| `--accent-primary` | `#458ac9` | `Color(0xFF458AC9)` | Primary brand CTA, active icons, play circles |
| `--accent-soft` | `#8dc8f8` | `Color(0xFF8DC8F8)` | Accent gradients, timeline scrubber bar |
| `--accent-lavender`| `#d9d5ff` | `Color(0xFFD9D5FF)` | Gradient blend token, ambient blob 2 |
| `--accent-glow` | `rgba(141, 200, 248, 0.35)` | `Color(0x598DC8F8)` | Floating dock & button elevation glows |
| `--ink-primary` | `#1c2b3d` | `Color(0xFF1C2B3D)` | High-contrast headers, active labels |
| `--ink-secondary` | `#586c82` | `Color(0xFF586C82)` | Body text, unselected menu icons |
| `--ink-muted` | `#859bb0` | `Color(0xFF859BB0)` | Captions, time codes, secondary meta |
| `--glass-surface` | `rgba(255, 255, 255, 0.72)` | `Color(0xB8FFFFFF)` | Elevated app bar, floating dock background |
| `--glass-card` | `rgba(255, 255, 255, 0.65)` | `Color(0xA6FFFFFF)` | Content cards, settings cards, poster tiles |
| `--glass-border` | `rgba(255, 255, 255, 0.85)` | `Color(0xD9FFFFFF)` | Specular frosted rim stroke |
| `--glass-specular`| `rgba(255, 255, 255, 0.95)` | `Color(0xF2FFFFFF)` | Top-left specular rim highlight |
| `--pill-bg` | `rgba(255, 255, 255, 0.82)` | `Color(0xD1FFFFFF)` | Inactive filter chip background |
| `--pill-active-bg`| `#e1f0ff` | `Color(0xFFE1F0FF)` | Active filter chip background |
| `--pill-active-text`| `#2c689c` | `Color(0xFF2C689C)` | Active filter chip text color |
| `--aura-color-1` | `#91c9ff` | `Color(0xFF91C9FF)` | Ambient aura top-left radial blob |
| `--aura-color-2` | `#d9d5ff` | `Color(0xFFD9D5FF)` | Ambient aura right radial blob |
| `--aura-color-3` | `#f4f8ff` | `Color(0xFFF4F8FF)` | Ambient aura bottom radial blob |

### 1.2 Dark Theme Palette (`body.dark`)
| CSS Token | Hex / RGBA | Jetpack Compose `Color` | Usage |
| :--- | :--- | :--- | :--- |
| `--bg-primary` | `#0e1724` | `Color(0xFF0E1724)` | Deep cinematic midnight background |
| `--bg-secondary` | `#142032` | `Color(0xFF142032)` | Card recesses and search pill background |
| `--accent-primary` | `#76b5ef` | `Color(0xFF76B5EF)` | High-visibility sky blue accent |
| `--accent-soft` | `#4d8bc7` | `Color(0xFF4D8BC7)` | Secondary blue glow and borders |
| `--ink-primary` | `#f0f6ff` | `Color(0xFFF0F6FF)` | Crisp white/blue primary typography |
| `--ink-secondary` | `#abbdd3` | `Color(0xFFABBDD3)` | Readable secondary typography |
| `--ink-muted` | `#6e849e` | `Color(0xFF6E849E)` | Low-emphasis metadata text |
| `--glass-surface` | `rgba(22, 34, 52, 0.75)` | `Color(0xBF162234)` | Translucent dark frosted dock and bars |
| `--glass-card` | `rgba(26, 40, 60, 0.65)` | `Color(0xA61A283C)` | Frosted dark cards |
| `--glass-border` | `rgba(140, 185, 235, 0.16)` | `Color(0x298CB9EB)` | Soft blue specular glass rim |
| `--glass-specular`| `rgba(255, 255, 255, 0.12)` | `Color(0x1FFFFFFF)` | Specular reflection line |
| `--pill-bg` | `rgba(30, 46, 68, 0.8)` | `Color(0xCC1E2E44)` | Dark mode chip container |
| `--pill-active-bg`| `rgba(77, 139, 199, 0.3)` | `Color(0x4D4D8BC7)` | Selected chip background |
| `--pill-active-text`| `#bfe0ff` | `Color(0xFFBFE0FF)` | Selected chip text |

### 1.3 AMOLED Pure Black Palette (`body.amoled`)
| CSS Token | Hex / RGBA | Jetpack Compose `Color` | Usage |
| :--- | :--- | :--- | :--- |
| `--bg-primary` | `#000000` | `Color(0xFF000000)` | True 0% power black (OLED pixels off) |
| `--bg-secondary` | `#08080a` | `Color(0xFF08080A)` | Deep obsidian card recesses |
| `--accent-primary` | `#8ac3f7` | `Color(0xFF8AC3F7)` | Vibrant neon blue contrast accent |
| `--accent-soft` | `#396fa1` | `Color(0xFF396FA1)` | Subdued secondary blue |
| `--ink-primary` | `#ffffff` | `Color(0xFFFFFFFF)` | Pure white headers |
| `--ink-secondary` | `#b0b4bc` | `Color(0xFFB0B4BC)` | Off-white body |
| `--ink-muted` | `#6b707c` | `Color(0xFF6B707C)` | Muted meta labels |
| `--glass-surface` | `rgba(18, 18, 22, 0.88)` | `Color(0xE0121216)` | Ultra-dark frosted glass |
| `--glass-card` | `rgba(22, 22, 26, 0.78)` | `Color(0xC716161A)` | Obsidian frosted card tiles |
| `--glass-border` | `rgba(255, 255, 255, 0.11)` | `Color(0x1CFFFFFF)` | Minimal clean rim stroke |

---

## 📐 2. Corner Radii & Geometry Tokens

| CSS Variable | CSS Value | Compose `Dp` / `Shape` | Component Application |
| :--- | :--- | :--- | :--- |
| `--corner-sm` | `14px` | `14.dp` (`RoundedCornerShape(14.dp)`) | Badges, small chips, episode cards |
| `--corner-md` | `20px` | `20.dp` (`RoundedCornerShape(20.dp)`) | Navigation buttons, search input, poster thumbnails |
| `--corner-lg` | `28px` | `28.dp` (`RoundedCornerShape(28.dp)`) | Media cards, continue watching cards, episode tiles |
| `--corner-xl` | `36px` | `36.dp` (`RoundedCornerShape(36.dp)`) | Nav rail, Top AppBar, Hero showcase card, Bottom dock |
| `--corner-pill` | `9999px` | `CircleShape` | Filter pills, CTA buttons, status badges, play circles |

---

## 🪟 3. Glassmorphism & Elevation Parameters

| Token Name | Light Value | Dark Value | AMOLED Value | Compose Implementation |
| :--- | :--- | :--- | :--- | :--- |
| **Glass Card Fill** | `0.65` Alpha White | `0.65` Alpha `#1A283C` | `0.78` Alpha `#16161A` | Dynamic background modulated by `glassIntensity` slider |
| **Glass Surface Fill**| `0.72` Alpha White | `0.75` Alpha `#162234` | `0.88` Alpha `#121216` | Dock, Rail & App Bar container fill |
| **Rim Stroke Width**| `1.dp` | `1.dp` | `1.dp` | Border stroke on all frosted surfaces |
| **Rim Stroke Color**| `Color(0xD9FFFFFF)` | `Color(0x298CB9EB)` | `Color(0x1CFFFFFF)` | Dual-gradient specular border (top-left to bottom-right) |
| **Backdrop Blur** | `24.dp` – `28.dp` | `24.dp` – `28.dp` | `16.dp` | Hardware-accelerated RenderEffect on Android 12+ (API 31+) |
| **Subtle Shadow** | `rgba(62,105,154,0.08)` | `rgba(0,0,0,0.35)` | `None` | Soft drop shadow behind floating cards |
| **Elevated Shadow**| `rgba(45,87,136,0.14)` | `rgba(0,0,0,0.55)` | `rgba(0,0,0,0.90)` | Hero showcase & bottom dock floating elevation |

---

## ✍️ 4. Typography Scale (Plus Jakarta Sans)

| Role | Size (`sp`) | Weight | Letter Spacing | CSS Equivalent |
| :--- | :--- | :--- | :--- | :--- |
| **Hero Title** | `32.sp` – `42.sp` | `800` (ExtraBold) | `-0.035em` | `.hero-welcome h1`, `.featured-title` |
| **Section Title** | `22.sp` | `800` (ExtraBold) | `-0.02em` | `.section-title` |
| **Card Header** | `15.sp` – `16.sp` | `700` (Bold) | `-0.01em` | `.poster-info h4`, `.setting-title-block h3` |
| **Body Primary** | `14.sp` | `500` (Medium) | `0.0em` | `.featured-desc`, `input` |
| **Body Secondary** | `13.sp` | `500` / `600` | `0.01em` | `.details-desc`, `.section-subtitle` |
| **Navigation Label**| `14.sp` | `600` (SemiBold) | `0.0em` | `.nav-button` |
| **Badge / Kicker** | `10.sp` – `11.sp` | `800` (ExtraBold) | `0.16em` | `.featured-tag`, `.hero-welcome .kicker` |
| **Micro Timecode** | `11.sp` – `12.sp` | `600` (SemiBold) | `0.02em` | `.poster-badge`, `.player-time-row` |

---

## 📱 5. Responsive Multi-Device Breakpoints

| Breakpoint Range | Device Class | Layout Configuration |
| :--- | :--- | :--- |
| **`< 640dp`** | Mobile Portrait | Floating Glass Bottom Dock (`80dp` height with safe insets), no side rail. 2-column media grid. Compact hero banner. |
| **`640dp` – `720dp`** | Mobile Landscape | Floating Bottom Mini-Dock or compact side rail. 3-column media grid. |
| **`721dp` – `960dp`** | Tablet Portrait | Compact Icon-only Navigation Rail (`80dp` width) with logo and action icons. 3-4 column media grid. Top search app bar. |
| **`961dp` – `1280dp`**| Tablet Landscape | Semi-expanded Navigation Rail (`210dp` width). 4-column media grid. Full Hero showcase card. |
| **`> 1280dp`** | Large Tablet / Foldable | Full expanded Navigation Rail (`250dp` width) with labeled sections. 5-column media grid. |
