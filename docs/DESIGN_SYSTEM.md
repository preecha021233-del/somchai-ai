# Somchai Design System

**Version:** 0.1.0  
**Last Updated:** 2026-11-16  
**Owner:** Grok (UI/UX & Visual Identity)

---

## 1. Visual Identity

### Brand Personality
- Calm Intelligence
- Controlled Power
- Premium & Trustworthy
- Transparent (never hides state)

### Logo Direction
- Abstract “S” mark combined with a controlled circular element
- Dark background preferred
- Accent color used sparingly for emphasis

---

## 2. Color System

### Core Colors
| Token                  | Hex       | Usage                          |
|------------------------|-----------|--------------------------------|
| Background             | #0B0F14   | App background                 |
| Surface                | #151B23   | Cards, sheets                  |
| Surface Elevated       | #1E2630   | Modals, floating elements      |
| Primary                | #0F766E   | Main actions, brand            |
| Primary Light          | #14B8A6   | Highlights, icons              |
| Text Primary           | #F1F5F9   | Main text                      |
| Text Secondary         | #94A3B8   | Secondary text                 |
| Text Tertiary          | #64748B   | Captions, timestamps           |
| Border                 | #2A3441   | Dividers, card borders         |

### Semantic Status
| Status    | Color     | Container   | Meaning                     |
|-----------|-----------|-------------|-----------------------------|
| Normal    | #22C55E   | #14532D     | Healthy / Success           |
| Warning   | #F59E0B   | #78350F     | Attention required          |
| Critical  | #EF4444   | #7F1D1D     | Immediate action / Danger   |
| Info      | #3B82F6   | #1E3A8A     | Informational               |

---

## 2A. Approved Chat UI Surface (Figma reference)

The SOMCHAI chat screen uses a dedicated light surface based on the approved 390×844 Figma frame. This is an intentional screen-level exception to the dark-first administrative screens above.

| Element | Value |
|---|---|
| Chat background / header / composer | `#FFFFFF` |
| Assistant response card | `#EEF0F4` |
| Composer input fill | `#F0F0F2` |
| Main chat text | `#17171B` |
| Secondary/action text | `#77777D` |
| Online status | `#20A464` |
| User bubble / SOMCHAI avatar | Linear gradient `#7438E8 → #356DEB` |
| Chat bubble radius | `22dp` |
| Header avatar | `48dp` |
| Message avatar | `34dp` |
| Composer input / send control | Rounded pill / `58dp` circular send button |

Navigation stays in the top-right menu. The chat screen has no bottom tab bar. Assistant response actions (Copy, Like, Dislike, Read aloud and Share) are part of the assistant response card and must remain interactive, not decorative.

## 3. Typography

- **Primary Font:** System Default (Inter / SF Pro on device)
- **Monospace:** JetBrains Mono (for IDs, logs, code)

### Scale
- Display Large: 34sp / Bold
- Headline Large: 24sp / SemiBold
- Title Medium: 16sp / Medium
- Body Large: 16sp / Regular
- Body Medium: 14sp / Regular
- Label Small: 11sp / Medium + letter-spacing

---

## 4. Spacing & Layout

- Base unit: **4dp**
- Common spacing: 8, 12, 16, 20, 24, 32dp
- Card padding: 16dp
- Screen horizontal padding: 20dp
- Corner radius:
  - Cards: 16dp
  - Buttons: 12dp
  - Pills / Badges: 999dp

---

## 5. Components

### StatusBadge
- Always uppercase
- Semantic color coding
- Used for system state, task status, memory category

### SomchaiCard
- Surface or Elevated variant
- 1dp border
- 16dp padding
- 16dp radius

### PrimaryButton
- Height 48dp
- Primary color fill
- SemiBold 15sp

---

## 6. Screen Inventory (Complete)

1. **Dashboard** — System overview + quick access
2. **Security Center** — Permission gates + audit events
3. **Memory Management** — Search, stats, memory list
4. **Tasks & Automation** — Active / queued / completed
5. **Owner Control** — Critical toggles + approvals + kill switch
6. **Settings** — Appearance, behavior, system
7. Loading / Empty / Error states (to be expanded)
8. Notification patterns (to be expanded)

---

## 7. Design Principles Applied

- Dark-first for focus and modern feel
- Status always visible (never hidden)
- Owner Control is prominent and irreversible actions are clearly dangerous
- No decorative clutter
- High contrast for accessibility
- Consistent component language across all modules

---

## 8. Handoff Notes for Developers

- All colors live in `ui/theme/Color.kt`
- Typography in `ui/theme/Type.kt`
- Theme wrapper: `SomchaiTheme`
- Reusable components under `ui/components/`
- Screens are pure Compose and currently use placeholder data
- Navigation graph and ViewModels are next integration points

---

**Status:** Design System v0.1 complete and implemented in code.  
Ready for expansion into remaining states and full navigation.
