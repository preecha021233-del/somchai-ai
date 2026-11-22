# SOMCHAI AI — Target Architecture

**Status:** New baseline  
**Model:** Protected Core + Sandboxed Plugins  
**Platform:** Android-first

## 1. Trust zones

### Protected Core (immutable to the agent)
1. Chat and command input boundary.
2. LLM connector and secret handling.
3. Security policy and permission manager.
4. Plugin package validator and constrained sandbox runtime.
5. Smoke tests and activation gate.
6. Backup, version pinning, and rollback.
7. Append-oriented activity log and resource governor.

The application update/signing process, not the AI agent, is the only route for changing Protected Core. Runtime checks are defense-in-depth; they do not replace Android app signing and release controls.

### Mutable Extensions
- Declarative tools and plugin manifests.
- Dynamic UI definitions rendered from validated JSON.
- Long-term memory and lessons learned.
- Plugin-specific configuration and data.

Extensions have no direct filesystem access outside their assigned data directory and no direct access to secrets, policy internals, or core files.

## 2. Data flow

1. Owner sends a request through the chat UI.
2. Core classifies the request and checks policy.
3. LLM output is parsed as an untrusted proposal, never executed as code.
4. If a capability is missing, Core creates a candidate extension in the staging area.
5. Validator checks schema, size, operation allowlist, permissions, and smoke tests.
6. UI presents the exact manifest, requested permissions, and test result.
7. Owner grants a single-use approval for that candidate version.
8. Core snapshots the active version, promotes the candidate, and runs a health check.
9. Failure restores the previous version and records the outcome.

## 3. Plugin execution boundary

The initial runtime supports declarative plugin manifests and a finite set of built-in operations. It does **not** execute arbitrary Kotlin, shell commands, downloaded APKs, or unrestricted JavaScript. Adding a new executable runtime requires a separate threat model, OS-level isolation, and security review.

## 4. Storage zones

- `files/core/`: core-owned state and policy; extensions cannot write here.
- `files/plugins/staging/`: candidate manifests.
- `files/plugins/active/`: approved manifests.
- `files/plugins/backups/`: last-known-good snapshots.
- `files/plugin-data/<plugin-id>/`: per-plugin data.
- `files/memory/`: owner-inspectable memory records.
- `files/audit/`: activity records.

All paths are resolved under app-private storage and validated against path traversal.

## 5. Failure behavior

Default is deny. If policy, validation, approval, storage, or health checks fail, the candidate remains inactive. Rollback failure enters safe mode and blocks plugin activation.
