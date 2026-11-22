# Phase 1 — Baby SOMCHAI Progress

Updated: 2026-11-22

## Implementation status

Phase 1 implementation is being consolidated on `phase-1/local-brain-runtime`. Repository-level implementation is in progress. Latest CI run #45037203591 passed JVM tests, llama.cpp ARM64 native build, debug APK assembly and artifact upload. Real-device acceptance remains outstanding.

### Implemented

- Offline-first chat routing: ordinary messages use the internal processor; no external provider is called by default.
- Optional external provider route remains explicit via `/ai` and requires an owner-configured key.
- Deterministic internal intents cover greetings, capability description, help, task planning and local-memory commands. Unsupported requests are reported honestly.
- Task Planner classifies exam, travel, research, development, troubleshooting, writing and general requests into distinct deterministic plans without claiming that the steps were executed.
- TaskExecutionStore persists task plans and step states; chat can create a task and mark its first step in progress without claiming external execution.
- Expanded research with DuckDuckGo HTML web-result parsing, HTTPS-only link filtering and up to three actual page-read attempts; added parser tests.
- Added encrypted Learned Skills storage. The local model can extract a reusable procedure from an owner-provided successful experience; each candidate needs 2–10 test cases and explicit owner approval before it is injected into matching Brain prompts. Skills are declarative prompt data, not executable code.
- Added a Thai reasoning policy that instructs the local model to check intent, context, assumptions, contradictions, missing information, calculations, unsupported claims and fabricated sources; this prompt policy is guidance, not proof of correct reasoning. Added a bounded prompt builder so the native 2,048-token context is not flooded by unbounded chat history or learned-skill text; the current request is prioritized.
- Added an actual llama.cpp JNI inference adapter, pinned to upstream commit `8d60873dfb8619f2b5ed31adfbab07cccb40db5c`, and connected it to LocalFirstBrain. The app can download the official Qwen2.5-0.5B-Instruct Q4_K_M GGUF model, verify its SHA-256, store it privately, and load it on-device. The native integration passes ARM64 CI. Target-device model download, load and generation are not yet verified; until the model is installed and loaded, the deterministic InternalProcessor fallback remains active.
- Local memory supports add, search, list, delete, backup and owner-confirmed restore, with a maximum of 500 records.
- Production memory and backups use AES-256-GCM with a non-exportable Android Keystore key. Legacy plaintext memory/notes are migrated only after encrypted data is verified.
- Added owner-controlled encrypted file export/import. The exported ciphertext is bound to the same installation's Keystore key; it is not a cross-device migration format.
- Research now discovers URLs through DuckDuckGo Instant Answer, Wikipedia and DuckDuckGo HTML Web Search, parses public HTTPS result links, then attempts to open up to three linked pages with a bounded read-only WebPageReader. It extracts visible text, limits response size/time, validates redirects and rejects local/private destinations. If no page opens, it explicitly labels snippets as discovery hints rather than read evidence.
- Plugin validation allowlists only the implemented `web.search` operation and enforces its exact permission. Candidate approval is hash-bound, smoke-checked and rollback-aware. The operational storage budget excludes the separately managed local model weights, preventing the 491 MB model from exhausting the 50 MB plugin/tool data budget.
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
- GitHub Actions run #45036781218 passed JVM tests, native llama.cpp CMake/NDK ARM64 build and debug APK assembly for commit `2019246a51e5453edfdcc4b49b21ced930b57eac`. Debug artifact ID `14760117769`; ZIP SHA-256 `0a03f6b1a8d687c524d88394308aedf7f459053a986705eb1839233c4fca13de`.
- Latest GitHub Actions run #45037203591 passed for commit `50e6a8be9c1d5f64218ae318ae85da31349889de`, including the learned-skill AES-GCM encryption/plaintext-exposure/tamper-rejection tests, ARM64 native build, debug APK assembly and artifact upload. [Run #45037203591](https://github.com/preecha021233-del/somchai-ai/actions/runs/45037203591).
- Round 6 is recorded in `docs/TESTING/PHASE1_TEST_LOG.md`.
- The successful CI proves compile/test/package integration only; it does not prove model download, native inference, Thai quality or performance on the owner's device.
- The starter 0.5B model is intended to prove on-device inference feasibility. It is not evidence of advanced reasoning quality. Benchmarking and model-quality evaluation are still required.

## Final acceptance gates — not yet passed

| Capability | Implementation status | Final evidence required |
|---|---|---|
| Chat UI / Figma parity | Updated in code | Build and inspect on Android device at the target viewport; verify keyboard collapse and scrolling |
| Response actions | Wired in code | Tap every action on a real device; verify clipboard, feedback persistence, Thai TTS availability and Android share sheet |
| Internal processing | Deterministic processor plus local-first Brain; native llama.cpp/Qwen runtime builds successfully on ARM64 CI | Device checks with model installed, network disabled and no API key; verify ordinary chat uses local inference and reports fallback honestly |
| Task decomposition | Implemented with JVM tests | CI pass and device output check |
| Research | DuckDuckGo Instant Answer, DuckDuckGo HTML Web Search, Wikipedia and bounded HTTPS page reader; up to three page-read attempts | CI pass plus online device check; confirm at least two actual pages can be opened/read, source URLs/date labels, failures and untrusted-data handling |
| Local memory | Encrypted CRUD, backup/restore and encrypted export/import | CI pass; device persistence, Keystore, file round-trip and tamper rejection |
| Basic plugin development | Validation, approval, smoke checks, disable/enable and rollback | CI pass plus lifecycle, owner-approval, denial and rollback checks |
| Protection and recovery | App-private storage, permission checks, audit log and rollback; encrypted learned-skill storage has AES-GCM tamper tests | CI pass plus fault-injection and negative security checks on device |
| Learned Skills | Encrypted reusable prompt procedures, 2–10 test cases, explicit owner approval and deletion revocation | P40–P43; model-backed learning/evaluation and device workflow still required |
| Update compatibility | Package ID retained; versionCode 10 / versionName 1.9.0 | Verify APK package, version and SHA-256 signing certificate against the owner-signed baseline; install over existing app without uninstalling or clearing data |

## Known limits — do not overclaim

- The deterministic processor remains the fallback. A starter local LLM runtime (llama.cpp + Qwen2.5-0.5B-Instruct Q4_K_M) now passes ARM64 CI, but target-device model loading, Thai quality and performance are not yet accepted.
- Learned Skills are owner-proposed, model-drafted prompt procedures with smoke tests and explicit approval; they do not autonomously create new code, tools, or reasoning algorithms.
- Research is still bounded to DuckDuckGo Instant Answer, DuckDuckGo HTML Web Search and Wikipedia; it is not yet a full web-scale crawler or comprehensive source-verification system.
- The Android app sandbox and in-process rule engine are not a hardened arbitrary-code sandbox.
- Encrypted memory export is tied to the non-exportable Android Keystore key of the original installation. If that key is permanently lost or app data is cleared, exported ciphertext cannot be decrypted; there is no key-recovery or cross-device migration mechanism.
- No claim of Phase 1 completion until the final CI, device, security, memory, plugin lifecycle and update-compatibility acceptance checks pass.

## Final verification sequence

1. Run the complete JVM unit-test suite (latest run #45037203591 passed).
2. Build the debug APK.
3. Build the owner-signed release APK using the existing protected signing secrets.
4. Verify `com.somchai.ai`, versionCode 10, and signing-certificate continuity against the previous owner-signed release artifact.
5. Install the release APK over the existing v1.8.0 baseline app on a real Android device without uninstalling or clearing data.
6. Verify Figma UI layout, top-right menu, keyboard collapse, chat scrolling, all response actions, memory export/import, plugin lifecycle, research and offline behavior.
7. Fix any failures and repeat only the affected checks plus the full release gate.
8. Close Phase 1 only when every acceptance gate has evidence.

## Task lifecycle completion (2026-11-22)
- Extended durable task-step states with FAILED and CANCELLED while preserving existing PENDING, IN_PROGRESS, and COMPLETED records.
- Added explicit failure reasons, retry-to-pending, and cancellation transitions. Completed work cannot be cancelled; only active work can be marked complete or failed.
- Added bounded task-store validation (record count, file size, step count, goal/title lengths, UUIDs, timestamps, duplicate IDs) and atomic temporary-file replacement.
- Added a chat command adapter and routed `/task` commands from the chat UI:
  - `/task list`, `/task show <id>`
  - `/task start <id> <step>`, `/task complete <id> <step>`
  - `/task fail <id> <step> | <reason>`, `/task retry <id> <step>`, `/task cancel <id> <step>`
- Chat step numbers are 1-based. These commands track task state only; they do not claim to perform external work.
- Added lifecycle and command-adapter regression tests. Execution/build verification remains a later completion gate after the implementation work is finished.

- Added ranked retrieval of up to three relevant owner-saved memories using Thai-aware word boundaries; unrelated memory entries are not bulk-injected into chat.
- Extended `BrainRequest` and the bounded prompt builder to include selected saved memories as contextual data, explicitly not as instructions.
- Chat retrieves approved skills and relevant saved memories on an IO dispatcher before local inference, avoiding encrypted-store reads on the UI thread.
- Added tests for memory relevance selection and prompt inclusion/budget.
- Corrected prompt-budget accounting so memory, approved skills, and history are trimmed within a reserved budget; the current question and final response instruction can no longer be clipped by the final history section.

## Research pipeline completion work
- Split source discovery from page reading behind injectable provider/reader interfaces; the HTTP provider aggregates DuckDuckGo Instant Answer, DuckDuckGo Web Search, and Wikipedia candidates.
- Added a research coordinator that distinguishes discovered links from pages whose content was actually opened and read.
- Research output now marks PARTIAL unless at least two distinct pages were actually read; page/source failures are retained in the report and activity log.
- Added an optional local-model synthesis step grounded only in the first two read pages, with explicit untrusted-web-data instructions and [S1]/[S2] references. If the local model is unavailable or synthesis fails, raw source evidence is still returned.
- Moved Web Search approval enforcement into the BuiltinToolExecutor boundary and connected it to the active approved plugin manifest; UI checks alone are no longer the only guard.
- Added mockable coordinator cases for success, empty results, timeout, provider error, and unreadable pages.

- Added a JNI tokenizer-count operation and made local inference trim optional context according to the model's actual tokenization, rather than relying only on character counts.
- The native generator now reserves context capacity for the requested output token budget plus a safety margin; it rejects prompts that cannot fit instead of decoding into an exhausted context.
- Chat history no longer repeats the current user message in the prompt/provider request.
- Added observable model lifecycle states (not installed, downloading, loading, ready, failed) so the chat and settings screens reflect asynchronous startup loading instead of remaining stuck on their initial status.

## Deterministic Thai reasoning coverage
- Added direct Thai answers for thunder formation and backup-data advantages/risks.
- Extended change calculation to handle quantity × unit-price wording such as `3 ชิ้น ชิ้นละ 25 บาท จ่าย 100 บาท`.
- Added explicit contradiction detection for conflicting weekdays and selected direct logical opposites; added missing-input prompts for speed and cash-change questions.
- Source requests now state when no sources have actually been opened and direct the user to approved Web Search rather than inventing citations.
- Added focused regression cases for these exact behaviors.
- Chat header and settings now display truthful model states instead of a generic always-online/always-active label.
- Tightened optional provider configuration: HTTPS only, no embedded credentials/query/fragment, standard HTTPS port, no local hostnames/private IP literals, and bounded model/API-key lengths. The stored endpoint is revalidated when loaded.

## Implementation fixes — 2026-11-22 (continued)
- Fixed learned-skill extraction prompt line breaks so the local model receives real separated prompt sections instead of literal backslash-n text.
- Wrapped owner-supplied experience in explicit untrusted-data delimiters and instructed the model not to obey embedded instructions while extracting reusable steps.
- Extended the learned-skill test to assert the delimiters, untrusted-data warning and real newline formatting.
- Corrected WebPageReader redirect enforcement: the configured maximum is now exactly three redirects; a fourth redirect is rejected rather than accidentally being followed.
- Bounded learned-skill extraction input and evaluation prompts so long owner-provided procedures/test cases cannot silently crowd out the test request; evaluation now compares expected text without depending on whitespace.
- Added regression coverage for oversized skill procedures, prompt bounds and Thai output matching with whitespace variation.


## Permission boundary hardening — 2026-11-22
- Centralized the operation-to-capability matrix in PermissionManager; the core now rejects unknown operations and requires an exact permission set for each supported operation.
- PluginValidator delegates operation/permission binding to that core policy instead of maintaining a separate operation allowlist, preventing a manifest from adding unrelated capabilities to an otherwise allowed operation.
- External web text sanitization now removes control and bidirectional formatting characters and neutralizes common English and Thai instruction-override phrases before prompt use; payload remains bounded to 12,000 characters.
- Added regression tests for operation escalation, duplicate capabilities, bidi/control characters and Thai prompt-injection phrasing. These are implementation changes; CI/device acceptance still must run before claiming the gate passed.


## Runtime permission, plugin activation and planner hardening — 2026-11-22
- Moved the supported operation-to-permission mapping into PermissionManager. Every plugin operation now requires an exact core-defined capability set; unknown operations, duplicate permissions and permission escalation fail closed.
- Plugin runtime enumeration now ignores manifests marked disabled. Approval and reactivation reject disabled manifests, and activation verifies the resulting manifest is enabled before reporting success.
- External web text filtering now strips control/bidirectional formatting characters and neutralizes common English and Thai instruction-override phrases before local-model use.
- TaskPlanner no longer silently truncates goals beyond 1,000 characters. It rejects oversized/control-character input and recognizes additional common Thai research requests (หาข้อมูล, สืบค้น, เปรียบเทียบข้อมูล).
- Added regression tests for capability escalation, prompt-injection sanitization, Thai research intent and invalid task input. These changes are committed; the new workflow result and target-device checks are still pending.


## Local Brain context isolation and fallback resilience — 2026-11-22
- Saved-memory context is now explicitly marked as untrusted reference data, with XML-like delimiters and escaping for angle brackets so stored notes cannot close the data boundary and inject new system sections.
- Kept the closing memory delimiter inside the fixed prompt budget even when several long memories are selected.
- LocalFirstBrain now catches failures from model readiness checks and from the deterministic fallback itself; if both local inference and fallback fail, it returns a truthful safe fallback message rather than propagating the processor exception.
- Added regression tests for delimiter injection, long-memory truncation and simultaneous model/fallback failure.
- Automated CI for the latest code commit is running; no pass result is claimed yet.

## External research prompt-injection hardening — 2026-11-22
- Expanded the external-page sanitization denylist to cover additional English override patterns (developer/system instruction replacement, hidden system-prompt disclosure, and role reassignment) and Thai equivalents (requesting system/internal instructions and role reassignment).
- Added regression cases for the additional English and Thai patterns.
- These filters are defense in depth only: all external page content must still remain explicitly untrusted in the model prompt. CI execution for this commit is pending.


## Round 8 — 2026-11-22 — Native Brain compile repair and strict model-load gate

- Branch: `phase-1/local-brain-runtime`
- Prior failing workflow: [#45043127526](https://github.com/preecha021233-del/somchai-ai/actions/runs/45043127526)
- Prior failing commit: `f113e5c0c033800a819b606e39e46e384aef8219`
- Failure evidence: `NativeLlamaInferenceEngine.kt` could not access private `BrainPromptBuilder.MAX_CURRENT_CHARS`.
- Code changes: made the shared prompt limit accessible inside the internal Brain package; enforced valid GGUF + pinned size range + pinned SHA-256 before JNI model loading; centralized installer/runtime validation; added a test rejecting a small GGUF-shaped file.
- Relevant commits: `297ede6b51daa14eb955908626f62216114c4d47`, `a8f1de1331d2d710af07d593a9f431d48c4e9f0b`, `d9e1225d0e07c6a0568c8945c6be7045092b3a3e`, `14551e2bfe006ac4c802b72b14a1bafaab4276b2`.
- New CI result: PENDING.
- Target-device load/generation and P01–P12 quality: NOT RUN.
- Phase 1 acceptance: NOT ACCEPTED.
