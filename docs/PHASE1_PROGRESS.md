# Phase 1 — Baby SOMCHAI Progress

Updated: 2026-11-22

## Implementation status

Phase 1 implementation is being consolidated on `phase-1/local-brain-runtime`. Repository-level implementation is in progress; native-build CI and real-device acceptance remain outstanding.

### Implemented

- Offline-first chat routing: ordinary messages use the internal processor; no external provider is called by default.
- Optional external provider route remains explicit via `/ai` and requires an owner-configured key.
- Deterministic internal intents cover greetings, capability description, help, task planning and local-memory commands. Unsupported requests are reported honestly.
- Task Planner classifies exam, travel, research, development, troubleshooting, writing and general requests into distinct deterministic plans without claiming that the steps were executed.
- TaskExecutionStore persists task plans and step states; chat can create a task and mark its first step in progress without claiming external execution.
- Added an actual llama.cpp JNI inference adapter, pinned to upstream commit `8d60873dfb8619f2b5ed31adfbab07cccb40db5c`, and connected it to LocalFirstBrain. The app can download the official Qwen2.5-0.5B-Instruct Q4_K_M GGUF model, verify its SHA-256, store it privately, and load it on-device. This implementation is pending native CI and target-device verification; until the model is installed and loaded, the deterministic InternalProcessor fallback remains active.
- Local memory supports add, search, list, delete, backup and owner-confirmed restore, with a maximum of 500 records.
- Production memory and backups use AES-256-GCM with a non-exportable Android Keystore key. Legacy plaintext memory/notes are migrated only after encrypted data is verified.
- Added owner-controlled encrypted file export/import. The exported ciphertext is bound to the same installation's Keystore key; it is not a cross-device migration format.
- Research discovers URLs through DuckDuckGo Instant Answer and Wikipedia, then attempts to open linked HTTPS pages with a bounded read-only WebPageReader. It extracts visible text, limits response size/time, validates redirects and rejects local/private destinations. If no page opens, it explicitly labels snippets as discovery hints rather than read evidence.
- Plugin validation allowlists only the implemented `web.search` operation and enforces its exact permission. Candidate approval is hash-bound, smoke-checked and rollback-aware.
- Plugin rejection and approval now validate plugin IDs and candidate paths to prevent path traversal.
- Memory UI reads real local records and supports backup/restore plus owner-selected encrypted file export/import.
- Chat UI is being aligned to the approved Figma screen: white chat surface, top-right navigation menu, gradient user bubbles, assistant response card with labeled actions, and bottom composer. There is no bottom tab bar.
- Response actions are wired to clipboard, persisted local feedback, Thai text-to-speech readiness checks and Android share chooser.
- JVM tests cover processor intents, task planning, memory CRUD/persistence, encrypted export/import and tamper rejection, plugin manifest negative cases, plugin smoke checks and permission filtering.
- Application ID remains `com.somchai.ai`. Current development build is versionCode 10 and versionName 1.9.0. Owner signing configuration is unchanged.

## Local model integration added on `phase-1/local-brain-runtime`

- Native llama.cpp build integration through CMake FetchContent and JNI.
- Model installer downloads the official Qwen model over HTTPS, checks the GGUF header and pinned SHA-256, and rejects incomplete or oversized downloads.
- Settings page exposes an explicit model download/install action; the optional external provider remains separate.
- Added JVM tests for valid/invalid GGUF headers.
- New CI workflow builds the ARM64 native library and app. Result is pending; no build pass is claimed yet.
- The starter 0.5B model is intended to prove on-device inference feasibility. It is not evidence of advanced reasoning quality. Benchmarking and model-quality evaluation are still required.

## Final acceptance gates — not yet passed

| Capability | Implementation status | Final evidence required |
|---|---|---|
| Chat UI / Figma parity | Updated in code | Build and inspect on Android device at the target viewport; verify keyboard collapse and scrolling |
| Response actions | Wired in code | Tap every action on a real device; verify clipboard, feedback persistence, Thai TTS availability and Android share sheet |
| Internal processing | Deterministic processor plus local-first Brain boundary; local model unavailable | CI tests and device checks with network disabled and no API key; verify normal chat uses internal Brain and reports fallback honestly |
| Task decomposition | Implemented with JVM tests | CI pass and device output check |
| Research | Two search endpoints plus linked HTTPS page reader in code | CI pass plus online device check; confirm at least two actual pages can be opened/read, source URLs/date labels, failures and untrusted-data handling |
| Local memory | Encrypted CRUD, backup/restore and encrypted export/import | CI pass; device persistence, Keystore, file round-trip and tamper rejection |
| Basic plugin development | Validation, approval, smoke checks, disable/enable and rollback | CI pass plus lifecycle, owner-approval, denial and rollback checks |
| Protection and recovery | App-private storage, permission checks, audit log and rollback | CI pass plus fault-injection and negative security checks |
| Update compatibility | Package ID retained; versionCode 10 / versionName 1.9.0 | Verify APK package, version and SHA-256 signing certificate against the owner-signed baseline; install over existing app without uninstalling or clearing data |

## Known limits — do not overclaim

- The processor is deterministic and rule-based. A starter local LLM runtime has now been added on a development branch, but it has not yet passed CI or target-device benchmarking and must not be described as accepted.
- It does not autonomously learn new reasoning skills or modify its own code.
- Research is a bounded lookup against DuckDuckGo Instant Answer and Wikipedia, not comprehensive multi-source research.
- The Android app sandbox and in-process rule engine are not a hardened arbitrary-code sandbox.
- Encrypted memory export is tied to the non-exportable Android Keystore key of the original installation. If that key is permanently lost or app data is cleared, exported ciphertext cannot be decrypted; there is no key-recovery or cross-device migration mechanism.
- No claim of Phase 1 completion until the final CI, device, security, memory, plugin lifecycle and update-compatibility acceptance checks pass.

## Final verification sequence

1. Run the complete JVM unit-test suite.
2. Build the debug APK.
3. Build the owner-signed release APK using the existing protected signing secrets.
4. Verify `com.somchai.ai`, versionCode 9, and signing-certificate continuity against the previous owner-signed release artifact.
5. Install the release APK over the existing v1.8.0 baseline app on a real Android device without uninstalling or clearing data.
6. Verify Figma UI layout, top-right menu, keyboard collapse, chat scrolling, all response actions, memory export/import, plugin lifecycle, research and offline behavior.
7. Fix any failures and repeat only the affected checks plus the full release gate.
8. Close Phase 1 only when every acceptance gate has evidence.
