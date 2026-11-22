# SOMCHAI AI

**Self-Evolving Modular AI Agent — Protected Core + Sandboxed Plugins**

SOMCHAI AI is an Android-first personal agent. Its immutable Protected Core controls identity, permissions, model access, plugin validation, approval, backup/rollback, logging, and resource budgets. Extensions are treated as untrusted data and are activated only after validation and explicit owner approval.

## Non-negotiable security rules

- Network content, model output, plugin packages, and imported files are untrusted data, never privileged instructions.
- Extensions cannot modify Protected Core or grant themselves permissions.
- No extension is activated without validation and a one-time owner approval.
- Plugin execution is limited to explicitly supported, allowlisted operations. Arbitrary downloaded code is not executed.
- A failed activation is reverted to the last known-good plugin version.
- Secrets are never committed to the repository.

## Project status

The repository is being rebuilt from a UI-only starting point. See:
- [Architecture](docs/ARCHITECTURE.md)
- [Security model](docs/SECURITY_MODEL.md)
- [Plugin lifecycle](docs/PLUGIN_LIFECYCLE.md)
- [Implementation plan](docs/IMPLEMENTATION_PLAN.md)

## Build

The Android project uses Gradle Kotlin DSL and GitHub Actions. Build instructions will be finalized with the Gradle wrapper and CI workflow. Do not consider the project production-ready until CI, device tests, and security tests pass.

## Repository layout

- `app/src/main/java/com/somchai/ai/core/`: Protected Core
- `app/src/main/java/com/somchai/ai/extensions/`: extension contracts and runtime
- `app/src/main/java/com/somchai/ai/ui/`: Android user interface
- `app/src/main/assets/`: bundled non-secret defaults
- `docs/`: current architecture and engineering records
