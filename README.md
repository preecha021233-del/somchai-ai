# SOMCHAI FULL SYSTEM

**Android-first AI Personal Assistant**  
Secure Modular Architecture • Governed Self-Evolution • Owner-Controlled Execution

---

## Overview

Somchai is a high-trust, owner-controlled AI personal assistant designed for Android.  
It prioritizes **security**, **transparency**, and **user sovereignty** over autonomy theater.

Core principles:
- Owner has absolute control
- All critical actions require explicit approval
- Memory and tools are fully inspectable
- Self-evolution is governed and auditable
- UI never hides system state

---

## Architecture (High Level)

```
┌─────────────────────────────────────────────────────┐
│                   Owner Layer                       │
│         (Security Center + Owner Control)           │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│              Governance & Policy Engine             │
│     (Permissions, Approval Rules, Audit Log)        │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│                  Agent Core                         │
│   Reasoning • Planning • Tool Use • Memory Access   │
└───────────┬─────────────────────────────┬───────────┘
            │                             │
┌───────────▼──────────┐     ┌────────────▼───────────┐
│   Memory System      │     │   Tool & Automation    │
│ (Long-term + Working)│     │   (Sandboxed Execution)│
└──────────────────────┘     └────────────────────────┘
```

---

## Tech Stack (Android)

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3 (custom Design System)
- **Architecture**: Clean Architecture + MVI
- **DI**: Hilt
- **Local Storage**: Room + DataStore
- **Security**: Android Keystore, Biometric, StrongBox when available
- **Background**: WorkManager + Foreground Service (with clear user visibility)

---

## Design System

Located in `app/src/main/java/com/somchai/ai/ui/theme/` and `design-system/`

- Dark-first premium aesthetic
- Clear status semantics (Normal / Warning / Critical)
- High accessibility contrast
- Consistent 8pt grid

---

## Module Structure

```
com.somchai.ai
├── ui/
│   ├── theme/          # Color, Typography, Shape, Theme
│   ├── components/     # Reusable design system components
│   └── screens/
│       ├── dashboard/
│       ├── security/
│       ├── memory/
│       ├── tasks/
│       ├── owner/
│       └── settings/
├── core/               # Navigation, Base classes, Utils
└── domain/             # Use cases, Models, Repository interfaces
```

---

## Current Status

- [x] Project structure
- [x] Design System (Color, Typography, Theme)
- [x] Core Components
- [x] Dashboard Screen
- [x] Security Center Screen
- [x] Memory Management Screen
- [x] Task & Automation Monitor
- [x] Owner Control Panel
- [x] Settings
- [ ] Full navigation graph
- [ ] Backend / Agent core integration
- [ ] Real security policy engine

---

## License

Private — Owner controlled.
