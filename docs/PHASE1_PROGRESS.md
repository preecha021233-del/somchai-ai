# Phase 1 — Baby SOMCHAI Progress

Updated: 2026-11-22

## Implemented in this branch

- Offline-first chat routing: normal messages go to the internal processor; no external provider is called by default.
- Optional external provider route is explicit via the `/ai` command and requires an owner-configured key.
- Deterministic internal intent handling for greetings, capability description, help, task planning, and local-memory commands.
- Task planner decomposes research, development, troubleshooting and general requests into explicit steps. It describes a plan and does not claim to have executed it.
- Structured app-private local memory supports add, search, list, delete, backup and owner-confirmed restore, with a bounded maximum of 500 records.
- Research now queries DuckDuckGo Instant Answer and Wikipedia, displays source URLs and Wikipedia last-edit dates when supplied, and labels missing dates/coverage limits.
- Active plugins can be disabled by the owner; the manifest is retained in the app-private backup area for recovery.
- Plugin validation accepts only the `web.search` operation actually implemented by Core and enforces its exact required permission; unimplemented operations and unnecessary permissions are rejected.
- Local memory records now carry schema version 1; valid legacy records without a version are migrated without overwriting a file containing invalid records.
- Memory tab now reads and manages real local-memory records rather than displaying hard-coded sample memories.
- Added JVM unit tests for processor intents, task planning, and memory CRUD/persistence.
- Application ID remains `com.somchai.ai`; versionCode incremented from 1 to 2 and versionName from 1.0.0 to 1.1.0. Existing owner signing configuration was not changed.

## Current acceptance status

| Capability | Status | Evidence still required |
|---|---|---|
| Internal processing | Implemented, automated tests pending CI | CI pass and device tests with network disabled and no API key |
| Task decomposition | Implemented, automated tests pending CI | CI pass; verify plan output on device |
| Research | DuckDuckGo + Wikipedia lookup with source URLs and available Wikipedia edit dates | Automated source-parser tests, adversarial content tests and device validation |
| Local memory | Add/search/list/delete implemented | CI pass; device persistence across app restart and update |
| Basic plugin development | JSON validation, smoke checks, hash-bound approval, activation, disable and rollback; least-privilege permission matching | Lifecycle integration tests, tampering, denial and rollback tests |
| Protection and recovery | Existing app-private storage, permissions, audit log and plugin rollback retained | Fault-injection and adversarial tests |
| Update compatibility | Version increment and prior owner device update test recorded | Test this new v2 APK over installed v1 on device; verify certificate fingerprint |

## Known limits — do not overclaim

- The processor is deterministic and rule-based, not a general-purpose LLM and not human-level reasoning.
- It does not autonomously learn new reasoning skills or modify its own code.
- Unsupported requests are reported as unsupported; optional LLM use requires the explicit `/ai` command.
- Research is still a single DuckDuckGo Instant Answer endpoint, not comprehensive multi-source research.
- The Android app sandbox and in-process rule engine are not a hardened arbitrary-code sandbox.
- Memory is stored in app-private JSON Lines files. Backup/restore currently stays inside app-private storage; encrypted export/import and schema migrations are not yet implemented.
- No claim of Phase 1 completion until CI, device, security, memory persistence, plugin lifecycle and recovery acceptance checks pass.

## Next implementation work

1. Run CI and fix all compile/test failures.
2. Add encrypted memory storage/export/import and verify key recovery behavior.
3. Expand plugin operation tests and ensure owner approval is required.
4. Improve research result provenance and untrusted-data handling.
5. Add negative/security tests and stability tests.
6. Produce v2 owner-signed APK only after CI passes; verify package ID, version and signing certificate before asking the owner to install.
7. Run device acceptance tests, then close Phase 1 only when all five capability gates pass.
