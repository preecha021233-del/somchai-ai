# Phase 1 — Baby SOMCHAI Progress

Updated: 2026-11-22

## Implementation status

Phase 1 implementation work is consolidated on `phase-1/chat-ui-figma-redesign`. Final validation is intentionally deferred until the implementation pass is complete.

### Implemented

- Offline-first chat routing: ordinary messages use the internal processor; no external provider is called by default.
- Optional external provider route remains explicit via `/ai` and requires an owner-configured key.
- Deterministic internal intents cover greetings, capability description, help, task planning and local-memory commands. Unsupported requests are reported honestly.
- Task Planner decomposes research, development, troubleshooting and general requests into steps without claiming that the steps were executed.
- Local memory supports add, search, list, delete, backup and owner-confirmed restore, with a maximum of 500 records.
- Production memory and backups use AES-256-GCM with a non-exportable Android Keystore key. Legacy plaintext memory/notes are migrated only after encrypted data is verified.
- Added owner-controlled encrypted file export/import. The exported ciphertext is bound to the same installation's Keystore key; it is not a cross-device migration format.
- Research queries DuckDuckGo Instant Answer and Wikipedia, displays source URLs, includes Wikipedia last-edit dates when supplied, and labels missing dates and source-coverage limits.
- Plugin validation allowlists only the implemented `web.search` operation and enforces its exact permission. Candidate approval is hash-bound, smoke-checked and rollback-aware.
- Plugin rejection and approval now validate plugin IDs and candidate paths to prevent path traversal.
- Memory UI reads real local records and supports backup/restore plus owner-selected encrypted file export/import.
- Chat UI is being aligned to the approved Figma screen: white chat surface, top-right navigation menu, gradient user bubbles, assistant response card with labeled actions, and bottom composer. There is no bottom tab bar.
- Response actions are wired to clipboard, persisted local feedback, Thai text-to-speech readiness checks and Android share chooser.
- JVM tests cover processor intents, task planning, memory CRUD/persistence, encrypted export/import and tamper rejection, plugin manifest negative cases, plugin smoke checks and permission filtering.
- Application ID remains `com.somchai.ai`. The next build increments versionCode to 7 and versionName to 1.6.0. Owner signing configuration is unchanged.

## Final acceptance gates — not yet passed

| Capability | Implementation status | Final evidence required |
|---|---|---|
| Chat UI / Figma parity | Updated in code | Build and inspect on Android device at the target viewport; verify keyboard collapse and scrolling |
| Response actions | Wired in code | Tap every action on a real device; verify clipboard, feedback persistence, Thai TTS availability and Android share sheet |
| Internal processing | Implemented with JVM tests | CI tests and device checks with network disabled and no API key |
| Task decomposition | Implemented with JVM tests | CI pass and device output check |
| Research | Two source endpoints and source metadata in code | CI pass plus online device check; confirm source URLs/date labels and untrusted-data handling |
| Local memory | Encrypted CRUD, backup/restore and encrypted export/import | CI pass; device persistence, Keystore, file round-trip and tamper rejection |
| Basic plugin development | Validation, approval, smoke checks, disable/enable and rollback | CI pass plus lifecycle, owner-approval, denial and rollback checks |
| Protection and recovery | App-private storage, permission checks, audit log and rollback | CI pass plus fault-injection and negative security checks |
| Update compatibility | Package ID retained; versionCode 7 / versionName 1.6.0 | Verify APK package, version and SHA-256 signing certificate against the v1.5.0 owner-signed baseline; install over existing app without uninstalling or clearing data |

## Known limits — do not overclaim

- The processor is deterministic and rule-based, not a general-purpose LLM and not human-level reasoning.
- It does not autonomously learn new reasoning skills or modify its own code.
- Research is a bounded lookup against DuckDuckGo Instant Answer and Wikipedia, not comprehensive multi-source research.
- The Android app sandbox and in-process rule engine are not a hardened arbitrary-code sandbox.
- Encrypted memory export is tied to the non-exportable Android Keystore key of the original installation. If that key is permanently lost or app data is cleared, exported ciphertext cannot be decrypted; there is no key-recovery or cross-device migration mechanism.
- No claim of Phase 1 completion until the final CI, device, security, memory, plugin lifecycle and update-compatibility acceptance checks pass.

## Final verification sequence

1. Run the complete JVM unit-test suite.
2. Build the debug APK.
3. Build the owner-signed release APK using the existing protected signing secrets.
4. Verify `com.somchai.ai`, versionCode 7, and signing-certificate continuity against the previous owner-signed release artifact.
5. Install the release APK over the existing v1.5.0 app on a real Android device without uninstalling or clearing data.
6. Verify Figma UI layout, top-right menu, keyboard collapse, chat scrolling, all response actions, memory export/import, plugin lifecycle, research and offline behavior.
7. Fix any failures and repeat only the affected checks plus the full release gate.
8. Close Phase 1 only when every acceptance gate has evidence.
