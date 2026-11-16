# Developer Handoff — Somchai UI/UX

**Date:** 2026-11-16  
**From:** Grok (Design & UI Implementation)  
**To:** Core Development Team

---

## What Has Been Delivered

### 1. Design System (Code)
- `Color.kt` — Full token set (background, surface, primary, semantic status)
- `Type.kt` — Typography scale
- `Theme.kt` — SomchaiTheme wrapper (dark-first)

### 2. Core Components
- `StatusBadge` — Semantic status pills (Normal / Warning / Critical / Info / Idle)
- `SomchaiCard` — Consistent card with border + radius
- `PrimaryButton`
- `EmptyState`
- `LoadingState`

### 3. Screens Implemented (Compose)
- DashboardScreen
- SecurityCenterScreen
- MemoryScreen
- TasksScreen
- OwnerControlScreen
- SettingsScreen

### 4. Documentation
- `DESIGN_SYSTEM.md` — Full visual + token specification
- `SCREEN_INVENTORY.md` — Complete list of screens
- This handoff document

---

## Component Contracts

### StatusBadge
```kotlin
StatusBadge(
    status = StatusType.NORMAL | WARNING | CRITICAL | INFO | IDLE,
    text = "LABEL"
)
```

### SomchaiCard
```kotlin
SomchaiCard(elevated = false) {
    // content
}
```

### Theme Usage
Every screen must be wrapped in `SomchaiTheme { ... }` at the root.

---

## Next Recommended Steps for Dev Team

1. Wire Navigation (Navigation Compose) between the 6 main screens
2. Create ViewModels + State for each screen
3. Connect real data sources (Memory repository, Task engine, Policy engine)
4. Implement biometric / lock screen gate before Owner Control
5. Add proper Empty / Error / Loading handling with real data
6. Expand notification & in-app banner system

---

## Design Constraints (Do Not Break)

- Do **not** change Security Policy or Permission Model for visual reasons
- Status colors and meaning must remain consistent
- Owner Control + Kill Switch must stay highly visible and deliberate
- Dark theme is the only supported theme in v1

---

## File Locations

```
app/src/main/java/com/somchai/ai/
├── ui/
│   ├── theme/
│   ├── components/
│   └── screens/
│       ├── dashboard/
│       ├── security/
│       ├── memory/
│       ├── tasks/
│       ├── owner/
│       └── settings/
docs/
├── DESIGN_SYSTEM.md
├── SCREEN_INVENTORY.md
└── DEVELOPER_HANDOFF.md
```

---

**Status:** UI foundation is complete and ready for integration.
