# Implementation Plan — New Baseline

This replaces the former 40-phase / 7-volume / A–M planning scheme.

## Track 1 — Build foundation
- Gradle project, Android manifest, application entry point, CI build.
- Preserve reusable Compose components only after compilation is established.

## Track 2 — Protected Core
- Permission manager and untrusted-input boundary.
- LLM connector with user-provided credential storage.
- Activity log and resource budgets.
- Safe-state behavior.

## Track 3 — Extension lifecycle
- Strict declarative manifest format.
- Staging, validation, smoke checks, explicit approval.
- Atomic promotion, versioned backup, rollback.

## Track 4 — Owner experience
- Chat, candidate review, approval/deny, plugin inventory.
- Dynamic UI from validated declarative JSON.
- Memory inspection and deletion controls.

## Track 5 — Verification
- Unit tests for permission decisions, manifest validation, path containment, approval binding, rollback.
- GitHub Actions build and test.
- Device tests on the target Android version and hardware.
- Security review before enabling any additional execution capability.

## Completion rule
A feature is not complete because source files exist. It is complete only after successful CI, tests, and documented device verification. This repository must not be described as 100% complete until that evidence exists.
