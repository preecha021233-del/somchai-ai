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


## Round 9 — 2026-11-23 — Repair two unit-test failures
- Previous CI evidence: workflow run #45043583832 reported 112/114 unit tests passing; failures were `OfflineReasoningEngineTest.rejectsIncompleteChangeQuestion` and `TaskExecutionStoreTest.rejectsUnknownTask`.
- Root cause 1: after the cash-change parser returned no result for missing payment, the generic arithmetic parser incorrectly extracted numbers embedded in Thai prose. Added an intent guard so incomplete change requests do not fall through to generic expression parsing.
- Root cause 2: unknown task lookup used Kotlin `error()`, which throws `IllegalStateException`; the API/test contract expects `IllegalArgumentException`. Changed the explicit exception type.
- Regression coverage: the existing tests assert null for incomplete change requests and `IllegalArgumentException` for unknown task IDs.
- CI for the repair: PENDING. Phase 1 acceptance: NOT ACCEPTED.


## Round 10 — 2026-11-23 — Full-system prompt hardening and plugin lifecycle coverage
- Commits: `a0d44240e537f2ab983755773ba85b4059a8421f`, `f2925916aa98c9ea2a94a9bab50a90e73ec52f2d`, `6a67541ce32800f9cae9045af85ce220c9e6bc83`, `6d3093923317fce5439775fed3c955e5a277dc4c`.
- Corrected research summarization so untrusted webpage data is sanitized, escaped, and enclosed before local-model processing; corrected literal-newline formatting.
- Added RAII cleanup for the native llama.cpp sampler.
- Hardened learned-skill extraction/evaluation and approved-skill prompt boundaries.
- Fixed cash-change parsing for the Thai payment verb “ชำระ”.
- Added PluginManager tests for approval, disable/re-enable, changed-candidate hash rejection, and path traversal.
- CI #138: FAILED on one new regression-test assertion (the test inspected the second evaluation prompt although the hostile input was in the first). Native C++ and Kotlin compilation completed; 121 tests ran, 1 failed.
- The test now records all evaluation prompts and checks the first one. CI #139 is PENDING; result must be updated after completion.
- Android device inference, Thai quality/performance, real web-source acceptance, and release/update-signature gates remain NOT RUN. Phase 1 acceptance: NOT ACCEPTED.


## Round 11 — 2026-11-23 — Corrected full-system audit batch verified
- CI #138 failure was isolated to a test capturing only the final prompt while checking content present in the first prompt.
- Corrected the test to capture and inspect all evaluation prompts.
- Added PluginManager lifecycle tests and a test-only CorePaths factory without changing the runtime storage location.
- CI #139: SUCCESS — [workflow run](https://github.com/preecha021233-del/somchai-ai/actions/runs/45087102935).
- Verified commit: `6d3093923317fce5439775fed3c955e5a277dc4c`.
- `testDebugUnitTest`, `assembleDebug`, and ARM64 llama.cpp build all passed; debug APK artifact upload succeeded.
- No Android device was attached to CI. Device-level acceptance gates remain NOT RUN; Phase 1 acceptance remains NOT ACCEPTED.


## Round 12 — 2026-11-23 — Research test and Android instrumentation build repair

- Branch: `phase-1/local-brain-runtime`
- Verified commit: `d4a3625727fcd9caeb81efd30d8c52db8c87469e`
- CI run #45092099946: SUCCESS — https://github.com/preecha021233-del/somchai-ai/actions/runs/45092099946
- First defect: `BuiltinToolExecutorTest.approvedSearchReadsAndReportsActualPages` expected discovery titles (`Source A/B`) although the production report intentionally displays titles returned by the actual page reader. Corrected the assertions to validate the page-reader titles, source URLs and read evidence.
- Second defect exposed by the full build: the instrumentation test imported unavailable `assertExists`. Replaced it with Compose's `assertIsDisplayed` API.
- Verified: JVM unit-test Gradle task passed; ARM64 llama.cpp native build passed; debug APK assembled; Android instrumentation APK compiled and assembled; both artifacts uploaded.
- Device instrumentation execution: NOT RUN (CI only built the test APK).
- Phase 1 acceptance: NOT ACCEPTED; remaining device-level P01–P43 evidence is still required.


## Round 13 — 2026-11-23 — P16 task lifecycle audit integration

- Branch: `phase-1/local-brain-runtime`
- Implementation commits: `ddd763a863a43163f0048489ee45a5f5deb3bd5e`, `c4e89b1be4e0fe0aed94726e1d45e4d4ee5558ba`, `658a0240c80e0d4d9969fcfc9668aa8fc71fd8d7`, `a3c4ff67e393b0e81b76ee483d5fc85f087ab485`.
- Connected durable task transitions to ActivityLogger with privacy-minimized audit details.
- Added tests for create/start/fail/retry/start/complete/cancel event ordering and for audit callback failure not undoing persisted state.
- CI status: NOT YET VERIFIED for this batch. No test pass is claimed.
- Device restart/persistence evidence: NOT RUN. P16 and Phase 1 remain NOT ACCEPTED.

## Round 14 — 2026-11-23 — P21 safe memory deletion

- Branch: `phase-1/local-brain-runtime`
- Commits: `8a5da393709a74455a0f5522b610146b9348139b`, `64389bd4d8a1f00ed28e569888993df3dbd04371`.
- Changed `/forget` to prefer exact full IDs and reject ambiguous short prefixes instead of silently deleting the first match.
- Added a regression test with two legacy memory UUIDs sharing the same displayed prefix; verifies ambiguity leaves both intact and full-ID deletion removes only the selected entry.
- CI: NOT YET VERIFIED. Device acceptance: NOT RUN. P21 and Phase 1 remain NOT ACCEPTED.

## Round 15 — 2026-11-23 — P20 transactional encrypted memory

- Commits: `9e712218af01619d53710ec270a154717b67847e`, `3e1614b02cf4863e08ba99ddafbf2a141af01e51`, `d258d33a34ee6aad6559f5ede81c48dea1e9bc22`.
- Candidate ciphertext is decrypted and schema-validated before replacing the live file. A rollback file is retained until committed bytes are read back successfully.
- Startup recovers interrupted replacements; tests cover candidate verification failure and interrupted rename recovery.
- CI: pending. Device acceptance: NOT RUN. P20 and Phase 1 remain NOT ACCEPTED.

## Round 16 — 2026-11-23 — P16/P43 task and skill persistence hardening

- Commits: `4dfd52d0a0c2a4aa0150af2afc57d3c74f1d6a58`, `8a48b8bce489fc1d5825b78cbd2079e39336555c`, `ea016c76a0152444ac67ad2426fdf17ab8872182`, `058c21a3b43be6a54b1918554d307c19a9a2f111`, `c9d20c4a09a4e09eb33ee2a6d7ec74925d904188`, `a13f22e6f7ad432c9efeab743e3795918febeb1a`, `3bf5871ffc32923919833064bed248d6bb0e8a9c`.
- Task store now uses staged JSON validation, rollback, read-back verification, and startup crash recovery.
- Learned-skill store validates persisted structure and approval invariants, uses verified encrypted replacement with rollback/recovery, and refuses empty/oversized retrieval queries.
- Added regression coverage for task and skill crash recovery, malformed skill records, and empty skill retrieval.
- CI: pending for this batch. Device acceptance: NOT RUN. P16/P43 and Phase 1 remain NOT ACCEPTED.

## Round 17 — 2026-11-23 — P26 provider-network and audit-log hardening

- Commits: `453d354d432ef3ae2c133c623aaeb23a3382544c`, `f4afdb94eedf75099213358e5497b50b42e8224f`, `9f0c6fd6b14585271a2b56fe5b250be19e84d4f6`, `dc215337185031554107a23ad7bb5063128ffa14`, `860855e48fe2d7b24afcb3290b712a4a67602ea6`.
- Optional provider calls now enforce public DNS resolution, disallow redirects, validate user prompt length, and bound response bodies.
- ActivityLogger now rotates at 1 MB, retains 1,500 lines, and reads only a bounded tail for oversized legacy logs.
- Added provider-host and audit-log retention regression tests.
- DNS resolution is not socket-pinned, so DNS rebinding protection is not claimed. CI/device verification pending.

## Round 18 — 2026-11-23 — P16 exact task-ID lookup

- Commits: `b14b987178acc15bf1431fe04bdac9a9ba445b5f`, `b547f63d28cdc04bfb30de7c40363c38192e5c71`.
- Exact full task UUIDs now resolve deterministically even if another task shares the prefix. Ambiguous short prefixes are still rejected.
- Added regression test for exact IDs and an ambiguous shared prefix.
- CI: pending. Device acceptance: NOT RUN. P16 and Phase 1 remain NOT ACCEPTED.

## Round 19 — 2026-11-23 — P17–P21 memory input bounds

- Commits: `9b7dd9921349ae236c2cc8655234ab7543feffd6`, `5c010a7757dff19eed58d2113d62774fdd4b8c2f`, `93471e704d0698b1898e00768d36b393b020b40d`.
- Memory reads now enforce a 1 MiB encrypted/decrypted limit, maximum 500 records, unique UUIDs, and an 8 KiB record bound.
- Backup restore and legacy migrations check input size/count before reading; oversized legacy sources remain untouched.
- Added oversized-store and duplicate-import regression tests. CI: pending. Device acceptance: NOT RUN. Phase 1 remains NOT ACCEPTED.


## Round 20 — 2026-11-23 — Research audit-log privacy minimization

- Commit: `9ed76eaea260f46dcc2984461a76e23765087293` (follow-up test-path correction: `804beaa294d75e25fa887d94eddc9b5704606e86`).
- Activity logs no longer store raw research queries, discovered/read URLs, provider error text, or page-reader exception text. They retain aggregate counts and query length only.
- Research result content still displays source URLs and errors to the user; this change limits persistent audit-log exposure, not the user-visible research report.
- Added a regression test containing a private query and URL and asserting neither is present in the persisted activity log.
- CI #205 later completed successfully for commit `804beaa294d75e25fa887d94eddc9b5704606e86`; the JVM unit-test task, ARM64 native build, debug APK, Android instrumentation APK build, and artifact uploads passed. The device instrumentation was not executed. Later IPv6, skill-integrity, research-source and arithmetic changes require their own latest-branch CI. P26 and Phase 1 remain NOT ACCEPTED pending full latest-branch and device/network validation.


## Round 21 — 2026-11-23 — Reserved IPv6 destination filtering

- Commit: `adddf7a87c693e76f4f055b8a6ef9e70d0d10323`.
- Tightened both the research page reader and model downloader to reject IPv6 IETF protocol-assignment, 6to4 transition, and 3fff documentation ranges, in addition to existing private, loopback, link-local, multicast and 2001:db8 documentation checks.
- Added regression tests for each denied range and a public IPv6 literal acceptance case in both URL validators.
- CI for this commit: pending. DNS answers are still not socket-pinned; DNS rebinding is not claimed solved. P26/P36 and Phase 1 remain NOT ACCEPTED.


## Round 22 — 2026-11-23 — Learned-skill evaluation integrity

- Commit: `773def4691ff4cf40b9c50f9db2fd7afd9215d31` (test coroutine import correction follows).
- Skill proposals now reject one-character triggers and duplicate normalized test inputs; encrypted persisted records enforce the same invariants.
- Evaluation now compares the current stored skill with the exact version tested before committing results. A concurrent/stale evaluation cannot overwrite newer results or revoke a more recent approval.
- Added regression coverage for short triggers, duplicate cases and stale concurrent evaluation.
- CI: pending. P40–P43 and Phase 1 remain NOT ACCEPTED.


## Round 23 — 2026-11-23 — Backward-compatible learned-skill safety migration

- Follow-up to Round 22: older encrypted skill records are preserved even if they contain one-character triggers or duplicate test inputs. Such previously approved records are treated as inactive when loaded, and cannot be re-approved until re-proposed with valid triggers and unique tests.
- Added regression coverage proving legacy data remains readable but cannot enter retrieval or be re-approved under the weaker criteria.
- New proposals continue to enforce minimum trigger length and unique test inputs. CI pending.


## Round 24 — 2026-11-23 — Distinct research source counting

- Research candidates and successful page reads are now deduplicated by canonical URL identity: fragments, hostname case and default HTTPS/HTTP ports no longer make one page look like multiple sources.
- The two-source completion predicate uses the same canonical identity rule.
- Added regression coverage proving two fragment/default-port variants of one page count as one read and cannot satisfy the two-source minimum.
- CI pending for the consolidated latest implementation; P24 and Phase 1 remain NOT ACCEPTED.


## Round 25 — 2026-11-23 — Learned-skill storage read bounds

- Skill storage now checks the encrypted file length before reading it into memory and checks the decrypted payload size before JSON parsing.
- Added a regression test for an oversized encrypted skill file. This closes a pre-validation memory-allocation gap in the P43 persistence boundary.
- CI pending for the consolidated branch.


## Round 26 — 2026-11-23 — Learned-skill audit lifecycle guarantees

- Connected regression coverage to the full skill lifecycle: proposal, evaluation, approval and deletion events are emitted without recording procedure or test-input payloads.
- Audit callback failures are isolated so they cannot turn a persisted skill transition into a reported failure.
- Added tests for event ordering, payload minimization and logger failure isolation. CI pending.


## Round 27 — 2026-11-23 — Bounded deterministic arithmetic

- Arithmetic parsing now rejects inputs with more than 128 numeric tokens, numeric literals longer than 64 digits, expressions longer than 512 characters, or more than 256 expression tokens.
- Added regression tests for excessive numeric literal length and token count to prevent pathological BigDecimal parsing on-device.
- CI pending.


## CI result update — 2026-11-23

- Run #45094274023 (#205): **SUCCESS** for commit `804beaa294d75e25fa887d94eddc9b5704606e86`.
- Verified JVM unit tests, ARM64 llama.cpp native build, debug APK assembly, Android instrumentation APK compilation/assembly, and both artifact uploads.
- The instrumentation test APK was built but not executed on a connected device.
- This result verifies code through the privacy-safe research audit-log change, but does not verify later commits for IPv6 destination filtering, learned-skill evaluation/audit, canonical research source counting, or arithmetic resource limits.


## Round 28 — 2026-11-23 — Reasoning acceptance precision

- Added an explicit P09 multi-step Thai budget problem regression test that checks both item subtotals and the final remaining balance.
- Narrowed weekday contradiction detection to require an explicit conflict connector and a repeated event anchor, avoiding false contradiction claims for different events on different days.
- Added a negative regression test for “today Monday, tomorrow Tuesday”. CI pending.


## CI failure and repair — 2026-11-23

- CI #213 failed during Kotlin test-source compilation at `OfflineReasoningEngineTest.kt:38`: the newly appended test file ended with literal backslash-n characters, producing a top-level syntax error.
- This was a test-file formatting defect; the workflow did not reach the unit-test execution/native build stages. Removed the stray characters and queued a fresh full workflow.
- No test pass is claimed for the new arithmetic, IPv6, skill-integrity or source-deduplication changes until the new run completes.


## Test-source correction committed — 2026-11-23

- Fix commit: `04f276245abb0bcd8b8903d7e0d6d9665a604f14`.
- Removed the literal trailing escape sequence from `OfflineReasoningEngineTest.kt`.
- CI #214 was superseded/cancelled; fresh full workflow #215 is queued for the corrected head. Results remain pending.


## CI #215 failure analysis — 2026-11-23

- JVM unit tests reached 172 cases; one test failed: `staleConcurrentSkillEvaluationCannotOverwriteNewerResults`.
- The production stale-write guard correctly threw when an old evaluation tried to overwrite newer results. The test used a regular `async` child, whose expected exception cancelled its parent `runBlocking` scope even though `await()` was caught.
- The test is being corrected to isolate the expected child failure in `supervisorScope`; this is a test-harness correction, not a relaxation of the stale-write protection.


## Round 29 — 2026-11-23 — Research summary citation validation

- Local-model research summaries are now shown only when their source IDs are valid for the pages actually read. With two or more readable pages, the summary must cite both [S1] and [S2], and may not cite unavailable IDs.
- Invalid or missing citations cause the model summary to be rejected; the actual source-page evidence remains visible and the user receives an explicit warning.
- Added unit coverage for valid, missing, extra and single-page citations. CI pending.


- Citation regex corrected to match literal `[S1]` / `[S2]` source IDs using Kotlin raw-regex escaping. CI remains pending.


## Phase 1 Gap-Fill integration — 2026-11-23

The supplied gap-fill package was merged selectively. Existing protected research, plugin, memory, model and task implementations were retained; security-sensitive replacement examples were not copied over them.

- Added ChatOrchestrator for ordinary local chat, explicit /search, explicit /ai and /task goal creation.
- Added ResearchResultVerifier labels READ_MULTI_SOURCE, READ_SINGLE_SOURCE, DISCOVERY_ONLY and NONE based on successfully read distinct URLs. Integrated the verifier into the existing bounded research pipeline without replacing URL validation, private-network protections, plugin approval checks, untrusted-data sanitization or citation validation.
- Added ResultEvaluator for empty output, known unsupported side-effect claims and plan disclaimers.
- Extended InternalProcessor with /planpersist and /tasks while preserving ambiguity-safe /forget.
- AgentApp is now wired through runtime.orchestrator for normal chat, /search and /ai; skill, plugin-status and task lifecycle commands retain their existing dedicated handlers.
- This is not device acceptance evidence. Phase 1 remains NOT CLOSED.

### Final device test sequence

1. Install the owner-signed update over the existing app without uninstalling or clearing data.
2. Disable Wi-Fi and mobile data; test สวัสดี and คุณทำอะไรได้บ้าง.
3. Test /plan พัฒนาแอป SOMCHAI and verify the not-executed disclaimer.
4. Test /task พัฒนาแอป SOMCHAI and /task list; verify persisted task and step 1 in progress.
5. Test /remember, /recall, /forget, backup/restore and encrypted export/import.
6. Before approving Web Search, test /search Kotlin and confirm it is blocked. Approve web-search, retry, and verify evidence label plus two distinct pages actually read.
7. Test /ai hello without provider configuration; confirm it is blocked. If configured, verify only explicit /ai calls the provider.
8. Test task lifecycle commands and plugin approve/reject/disable/enable/rollback.
9. Test copy, feedback, Thai TTS and Android share actions.

**Phase 1 close decision: NOT CLOSED.** Device evidence, owner-signed update compatibility and P01–P43 acceptance results remain required.


## Gap-Fill CI verification — 2026-11-23

- Verified commit: d6e1c93f5f094b8e666e0269b9310ad18d0288fe
- GitHub Actions run #225: SUCCESS — https://github.com/preecha021233-del/somchai-ai/actions/runs/45100829228
- ./gradlew testDebugUnitTest: PASS (BUILD SUCCESSFUL).
- ./gradlew assembleDebug assembleDebugAndroidTest: PASS (BUILD SUCCESSFUL).
- Native llama.cpp ARM64 CMake/NDK build: PASS.
- Debug APK artifact uploaded: ID 14785207125.
- Android instrumentation test APK artifact uploaded: ID 14785017835.
- Instrumentation tests were compiled and packaged only; they were NOT executed on a physical Android device.
- assembleRelease was not run because the workflow does not have the owner's local signing credentials. No signing key was changed or generated.
- This CI result verifies JVM tests and build/package integration for the recorded commit. It does not prove on-device model inference, Thai answer quality, UI interaction, existing-install update compatibility, or final P01–P43 acceptance.


## DNS Rebinding / TOCTOU remediation — 2026-11-23

- Replaced the WebPageReader HTTP transport with direct TCP socket + layered TLS transport. The main read path no longer uses URL.openConnection / HttpsURLConnection.
- For each URL hop, WebPageReader validates HTTPS-only scheme, rejects userinfo and non-443 ports, blocks localhost/.local/.internal/.lan, resolves the host once, rejects the complete DNS answer if any address is not public, then connects the raw socket using InetSocketAddress(pinnedIp, 443).
- The TLS socket is layered over the already-connected raw socket. SNI and HTTPS endpoint identification use the original URL hostname; HTTP Host uses the URL authority. Certificate validation happens during TLS handshake before the HTTP request is sent.
- Redirect following is manual. Every Location is resolved relative to the previous URI, then revalidated and separately resolved/pinned. No automatic redirect handler is used.
- Preserved and extended IPv4/IPv6 filtering: loopback, unspecified, private/site-local, link-local, multicast, CGNAT, metadata 169.254.0.0/16, documentation, benchmarking, 6to4, protocol assignment and reserved ranges.
- HTTP parser is bounded: connect/read timeouts, 32 KiB header limit, 1 MB body limit, chunked/content-length/close-delimited bodies, and HTML/XHTML/plain-text content types only. Requests send Accept-Encoding: identity and Connection: close.
- Added/expanded WebPageReaderSecurityTest for private/local URL rejection, HTTPS-only, userinfo, non-443 port, private redirect rejection, mixed public/private DNS answer rejection, rebinding simulation proving TCP destination remains the validated InetAddress, SNI hostname and HTTPS endpoint-identification configuration, and special IPv4/IPv6 ranges.
- Added docs/TESTING/OWNER_RELEASE_BUILD.md with local-only owner-keystore setup, version/package/certificate verification and safe adb install -r update steps. Added docs/TESTING/PHASE1_DEVICE_FINAL_CHECKLIST.md with A/B/C device acceptance tables. Added .gitignore rules for signing materials and local build files.
- Security implementation changes only touch WebPageReader and its focused tests; no applicationId, signing config, Protected Core, orchestrator, provider-default or plugin-approval policy changes were intended.

### CI evidence for this remediation

- Earlier CI run #233 (run ID 45102529619), commit 3a18b57d087115052400cde7f28f32a59efc5150, failed at JVM compilation. The compiler reported that the custom SSLSocketFactory omitted an abstract overload and the HTML extractor contained an unsupported escape sequence. That implementation was subsequently replaced with direct pinned socket transport, removing the custom SSLSocketFactory and fixing the extractor.
- CI run #237 (run ID 45103440086), commit e274cf6dbf9d0fb7690ed5267ce8187d4ca74a6e, failed compilation because the TLS configuration helper had not yet been inserted at the call site. This was corrected in the following commit.
- CI run #238 (run ID 45103601911) completed SUCCESS for code commit 014e0f6583ebd692ee3b2b911c2e5bcfaff02a9f. Verified log markers: testDebugUnitTest — BUILD SUCCESSFUL in 54s; assembleDebug + assembleDebugAndroidTest — BUILD SUCCESSFUL in 1m 38s; ARM64 native build completed; both artifacts uploaded. Debug artifact ID 14786671986 (22,822,700 bytes; ZIP SHA-256 006e3f65e4e823d9e2801362839077a9e11c4ba189084b0a9a1e7dcf936ba099). Instrumentation APK artifact ID 14786712130 (855,969 bytes; ZIP SHA-256 59c01dfc4b74c45352e6150046d9004ff1554c930148a95d7a3807318332a6bc). [Run #238](https://github.com/preecha021233-del/somchai-ai/actions/runs/45103601911).
- Device TLS handshake, live DNS rebinding, app update/signature continuity and on-device behavior remain outstanding; CI success is not device acceptance.
- Device TLS handshake, live DNS rebinding and actual Android behavior remain required even after CI passes.

- Owner release procedure: [OWNER_RELEASE_BUILD.md](OWNER_RELEASE_BUILD.md). Final device test matrix: [PHASE1_DEVICE_FINAL_CHECKLIST.md](PHASE1_DEVICE_FINAL_CHECKLIST.md). The checklist is prepared but all real-device rows remain BLOCKED until executed and evidence is recorded.


### Final HTTP trailer bound follow-up

- Added a cumulative 32 KiB cap for chunked-response trailers so the HTTP header-size limit also covers trailers, not only the initial header block.
- CI run #239 (run ID 45104053378), code commit 537caa649733a468a3fb1d26e8af30f82bc85cc3: JVM unit tests passed; ARM64 native runtime + Debug APK / instrumentation APK build step is still in progress at the time of this update. Final acceptance remains pending until this latest run completes successfully.


## Phase 1 — Chunk 1: Thai understanding gap assessment (2026-11-23)

Scope: compare P01–P08 with current implementation and tests; identify gaps only. This is a source/test review, not a claim that the full acceptance group has passed.

| Criterion | Existing implementation / evidence | Review finding |
|---|---|---|
| P01 greeting | `InternalProcessor.isGreeting`; `InternalProcessorTest.answersGreetingWithoutExternalProvider`; `ChatOrchestratorTest.greetingUsesInternalAndDoesNotCallRemote` | Deterministic Thai greeting exists; orchestrator test asserts remote-provider call count remains zero. |
| P02 help | `InternalProcessor.helpText`; `InternalProcessorTest.helpListsAllChatCommandFamiliesAndTheirOwnerBoundaries` | Help output exists and test checks major command families, but it does not assert the exact complete supported-command list. Genuine test-coverage gap; address in Chunk 2/4 only if still applicable after focused test design. |
| P03 arithmetic precedence | `OfflineReasoningEngine.solveArithmetic`; `InternalProcessorTest.evaluatesArithmeticPrecedenceParenthesesAndUnarySigns` | Required expression 2 + 3 * 4 is covered with expected 14. |
| P04 parentheses | Same test; case `(2 + 3) * 4` expected 20 | Covered. |
| P05 nested expression | Same test; case `((2 + 3) * (4 - 1)) / 3` expected 5 | Covered. |
| P06 Thai cash-change word problem | `OfflineReasoningEngine.solveChangeProblem`; `InternalProcessorTest.solvesQuantityAndUnitPriceCashChangePrompt`; `OfflineReasoningEngineTest.calculatesChangeFromQuantityAndUnitPriceInThai` | Required 3 × 25 and 100 − 75 = 25 calculation is covered. |
| P07 thunder explanation | `BasicKnowledgeEngine.answer`; `BasicKnowledgeEngineTest.explainsHowThunderIsProduced`; `InternalProcessorTest.answersThunderQuestionWithCausalExplanation` | Thai causal explanation and key terms are covered by tests. |
| P08 backup pros/cons | `BasicKnowledgeEngine.answer`; `BasicKnowledgeEngineTest.explainsBackupAdvantagesAndRisks`; `InternalProcessorTest.explainsBackupAdvantagesAndDisadvantages` | Multiple relevant advantages, disadvantages and encryption risk are present and tested. |

### Cross-cutting findings

- The deterministic P01–P08 paths are implemented and have focused unit-test coverage, but this review did not execute a fresh test run. Prior CI records in this log prove repository test/build runs for their recorded commits only.
- The acceptance suite separately requires at least 18/20 local-model prompts to meet the relevance/correctness rubric. The starter Qwen2.5-0.5B model's target-device installation/inference and quality scoring are not evidenced; that model-quality gate remains BLOCKED, not PASS.
- P02 exact help-list assertion is the only concrete P01–P08 deterministic coverage gap identified in this source review. No code change was made in Chunk 1.
- Source references reviewed: `InternalProcessor.kt` blob `86866268f8c790f6d04eea5c376df631c1f8c7a1`; `BasicKnowledgeEngine.kt` blob `e632b3e04ab640d3397bf36bfe947e651d5eb056`; `OfflineReasoningEngine.kt` blob `a2fb77a99b005ecec303a50cca603223eaef7ee7`; `InternalProcessorTest.kt` blob `9ae9502130e19aacc5c767e7989190890e0ce741`; `BasicKnowledgeEngineTest.kt` blob `443bbb7de3be38b9bc8d2d506424b8a68caf3427`; `OfflineReasoningEngineTest.kt` blob `afa54d0ba44397a6b018c1974d4bff4efa770ed9`; `ChatOrchestratorTest.kt` blob `57100249b4eafb25cc25d644f62ff14ceddaa3ef`.
- Chunk 1 is DONE as a review/documentation deliverable. This does not mark P01–P08 or Phase 1 accepted.


## Phase 1 — Chunk 2: P02 exact help-list coverage fix (2026-11-23)

- Replaced the previous partial `/help` test (which only checked selected command substrings) with an exact ordered-line assertion for the complete current help output.
- The expected list now covers `/help`, `/plan`, `/planpersist`, `/tasks`, all `/task` lifecycle operations, memory commands, approved Web Search, plugin listing, explicitly owner-configured `/ai`, all learned-skill commands, and the capability hint.
- This is a test-only change. No production behavior or command policy was changed.
- Commit: `dcfa1da821fc4e7dc3acfc1e899f205a51bfc3df`; file: `app/src/test/java/com/somchai/ai/core/processing/InternalProcessorTest.kt`.
- CI verification: GitHub Actions run #240 (run ID `45110203889`) — **SUCCESS** for commit `dcfa1da821fc4e7dc3acfc1e899f205a51bfc3df`. [Run #240](https://github.com/preecha021233-del/somchai-ai/actions/runs/45110203889).
- Chunk 2 is DONE: the exact `/help` assertion change is committed and the workflow completed successfully. This is not evidence that all P01–P08 criteria pass; the local-model 18/20 target-device quality gate remains BLOCKED.


## Phase 1 — Chunk 3: Thai intent extraction precedence (2026-11-23)

- Evidence-based gap: the deterministic classifier checked action-word substrings before interrogative cues. For example, "การบันทึกข้อมูลมีข้อดีอย่างไร" contains "บันทึก" and was incorrectly classified as a command rather than a question.
- Updated OfflineReasoningEngine.classify to recognize interrogative forms before action keywords, including "อย่างไร", "เท่าไร/เท่าไหร่", "อะไร", "ที่ไหน", "เมื่อไร/เมื่อไหร่", "กี่", "ทำไม", and "เพราะอะไร".
- Added a guard so explicit negative intent ("ไม่ต้องการ", "ไม่อยาก", "ไม่ให้", "อย่า") is not mislabeled as an executable command.
- Added regression tests for a question containing an action keyword, a negated delete statement, and additional Thai question forms.
- Production commit: b7b196848971392400e0db21769abf7f838fcb41.
- Test commits: e0574ad1a375cf49c85ede01495759a4bbbeafd7 and newline-literal correction 11c68bec872a4545d38a6876cc2e8ad4474ac2d3.
- CI: pending on the corrected test head. The earlier run triggered for the intermediate test commit was superseded/cancelled; no test pass is claimed yet.
- Chunk 3 remains IN_PROGRESS until the latest CI run completes successfully.

- CI verification: GitHub Actions run #243 (run ID 45110860508) — SUCCESS for corrected test commit 11c68bec872a4545d38a6876cc2e8ad4474ac2d3. [Run #243](https://github.com/preecha021233-del/somchai-ai/actions/runs/45110860508).
- Chunk 3 is DONE. This confirms the added classifier regression tests pass in CI; it does not claim the full P01–P08 acceptance group or the target-device model quality gate has passed.


## Phase 1 — Chunk 4: Incomplete-question clarification (2026-11-23)

- Evidence-based gap: the existing missing-information handler detected incomplete speed and cash-change questions but only said that at least one input was missing; it did not identify which specific input the user needed to provide.
- Updated speed calculation clarification to list missing distance and/or elapsed time individually.
- Updated cash-change clarification to list missing total price and/or payment amount individually, including both 'จ่าย' and 'ชำระ' forms.
- Added regression tests for both-missing and one-missing cases for speed and cash-change questions.
- Production commit: 25ce86bf48cf3ff3fbb2b0c7715be9b9fe9cbad8. Test commit: 8a3c477969a86a9031cae17f85a920c7b749da00.
- CI verification pending; chunk remains IN_PROGRESS until the workflow completes successfully.

- First CI attempt #245 failed: 201 JVM tests executed, 200 passed, 1 failed (`identifiesMissingCashChangeInputsIndividually`, line 77). The negative assertion searched the entire explanatory sentence, which necessarily names the required total-price input even when that field is not missing. Corrected the test to inspect only the `ตอนนี้ยังขาด:` segment. Test-fix commit: 7cc7f923c1342288fefdd6cc1780d167e0a5c7ab. CI rerun pending.


- CI rerun: GitHub Actions run #246 (run ID 45111802478) — **SUCCESS** for corrected test head 7cc7f923c1342288fefdd6cc1780d167e0a5c7ab. [Run #246](https://github.com/preecha021233-del/somchai-ai/actions/runs/45111802478).
- Chunk 4 is DONE. The clarification implementation and its corrected regression tests are committed and the full CI workflow passed. This closes Chunk 4 only; it does not establish P01–P08 or Phase 1 acceptance.


## Phase 1 — Chunk 5: P09 multi-step fact analysis review (2026-11-23)

- Reviewed the P09 acceptance target and current deterministic reasoning path.
- Existing end-to-end regression test: `InternalProcessorTest.solvesMultiStepThaiPurchaseBudgetProblem` uses a 500-baht budget, 2 notebooks at 30 baht each, and 3 pens at 10 baht each; it asserts both intermediate subtotals (60 and 30 baht) and the final remaining balance (410 baht).
- The production path `solveMultiStepBalanceProblem` calculates each item subtotal, sums the subtotals, checks insufficient budget, and returns the intermediate and final calculations.
- No new production change was justified: the reviewed P09 case is already implemented and covered by the existing regression test.
- CI evidence: GitHub Actions run #246 (run ID 45111802478) — **SUCCESS** for test head `7cc7f923c1342288fefdd6cc1780d167e0a5c7ab`; the P09 regression test is present in that tested tree. [Run #246](https://github.com/preecha021233-del/somchai-ai/actions/runs/45111802478).
- Chunk 5 is DONE as a review/evidence task. This does not declare all P09–P12 acceptance cases or Phase 1 accepted.


## Phase 1 / Chunk 6 — Contradiction detection (2026-11-23)

- Reviewed P10 and the current deterministic contradiction detector.
- Found over-broad checks: any text containing both “ไม่เคย” and “แต่เคย” was flagged, even when the destinations/actions differed; any text containing both “เลขคู่” and “เลขคี่” was flagged without confirming they referred to the same integer.
- Updated `OfflineReasoningEngine.detectContradiction` to require an explicit same-integer reference for parity conflicts and matching target text for “ไม่เคย…แต่เคย” conflicts.
- Added processor-level regression tests for same-claim contradiction, different destinations, different integers, and same-integer parity contradiction.
- Production commit: `efa5cc984bb407cd4024aaab35d5111b0d2f02e7`.
- Test commit: `674b126b35b2f28834f5d5bcd765808bb6649495`.
- CI run #247: pending at time of log update; do not mark Chunk 6 DONE until CI succeeds.
- Scope: Chunk 6 only. This does not claim P10 or the P09–P12 group is accepted.


## Phase 1 / Chunk 6 — Contradiction detection (2026-11-23) — completed

- Refined weekday contradiction detection to require multiple weekday claims to refer to the same event anchor and an explicit conflict connector. Distinct events such as today and tomorrow are not contradictions.
- Refined parity detection: an explicit integer is contradictory only when that same integer is asserted to be both even and odd; different integers are not conflated. Generic same-integer wording remains supported.
- Kept the never/ever contradiction scoped to matching target text rather than merely the presence of both phrases.
- Added processor-level regression coverage for same/different destinations, same/different integers, same-event weekday conflict, today/tomorrow, and insufficient weekday facts. Fixed the test fixture to use one processor instance so TemporaryFolder does not recreate the same directory twice.
- Production commits: `efa5cc984bb407cd4024aaab35d5111b0d2f02e7` (claim scope) and `8bcb2b23f1b69bb0bd62fbfe0ad9843414c4033c` (explicit-integer parity regex).
- Test commits: `e4cedbad69fa064cfacfdf775598f148b9a72b70` (fixture correction) and `f6875e59d869905fc812d669aa500178d7fc4985` (expanded regression matrix).
- CI: GitHub Actions run #252 (run ID `45112818101`) — **SUCCESS** for head `f6875e59d869905fc812d669aa500178d7fc4985`. JVM unit-test job completed successfully. [Run #252](https://github.com/preecha021233-del/somchai-ai/actions/runs/45112818101).
- Chunk 6 is DONE. This closes the work-chunk deliverable only; it does not declare P09–P12 or Phase 1 accepted.


## Phase 1 — Chunks 7–8: reasoning explanations and answer verification (2026-11-23)

- Implemented and committed on branch `phase-1/local-brain-runtime`; code/test head: `232e24bdb7d909fc2d9cc406c44c161614312f0f`.
- Chunk 7: contradiction explanations now name the exact conflicting integer and explain why the claims cannot both be true. Multi-step budget and cash-change responses include explicit calculation lines; missing-input clarification from Chunk 4 remains covered by its regression tests.
- Chunk 8: added `OfflineReasoningEngine.verifyArithmeticAnswer` and wired it into `ResultEvaluator` for `reasoning.arithmetic`. Displayed arithmetic equations are parsed and checked against their stated results; incorrect or unverifiable arithmetic is marked unsafe and replaced with a safe clarification instead of being returned as confirmed.
- Added `ResultEvaluatorTest` coverage for correct multi-step arithmetic, incorrect arithmetic, missing verifiable equations, and valid cash-change calculations; strengthened contradiction explanation assertions.
- CI GitHub Actions run #259 (run ID `45113740793`) — **SUCCESS** for head `232e24bdb7d909fc2d9cc406c44c161614312f0f`. JVM unit-test step passed; ARM64 native runtime and Debug APK plus Android instrumentation APK builds completed; both artifacts uploaded. [Run #259](https://github.com/preecha021233-del/somchai-ai/actions/runs/45113740793).
- Debug APK artifact ID `14790094169`; ZIP SHA-256 `440137bb87b53572b62bfeb01a561382d0c89f720a6a27c6976bbc020cffafdf`. Android instrumentation APK artifact ID `14791220095`; ZIP SHA-256 `798396202e2cca33a07977c5ef693c06b7491f903c8101805b06484e5bc6eff1`.
- Chunks 7 and 8 are DONE as work-plan deliverables. Group 2 (Chunks 5–8) is now recorded complete. This does **not** declare P09–P12 fully accepted: the complete acceptance matrix still requires explicit criterion-by-criterion evidence, and CI does not replace target-device checks or final Phase 1 acceptance.


## Phase 1 — Group 3 / Chunks 9–12: planning and task execution (2026-11-23)

- Reviewed P13–P16 acceptance targets against `TaskPlanner`, `TaskExecutionStore`, `TaskCommandProcessor`, `InternalProcessor`, `ChatOrchestrator`, and `CoreRuntime`.
- P13 exam plan: existing template already included lesson review, past papers, error review, and rest. Strengthened tests to assert every required step and its order.
- P14 travel plan: existing template already included route comparison, booking after owner approval, packing, and departure. Strengthened tests to assert every required step and its order.
- P15 Android development plan: existing template already included requirements/acceptance criteria, implementation, and tests. Strengthened tests for all three and for the user-facing `/plan` output.
- Task decomposition: added a regression ensuring a development request containing the substring “ทดสอบ” is classified as DEVELOPMENT rather than EXAM; this protects the Thai “สอบ” substring boundary.
- P16 task start: added an orchestrator integration test proving `/task <goal>` creates a durable task record, moves step 1 to IN_PROGRESS, leaves later steps PENDING, emits TASK_CREATED / TASK_STEP_STARTED / CHAT_TASK_CREATED audit events, and does not claim external work was performed. Reopening TaskExecutionStore preserves the task and IN_PROGRESS status.
- Task lifecycle coverage: extended persistence checks for COMPLETED and CANCELLED states after store reopen. Existing tests also cover sequential start/complete, failure reason persistence, retry, cancellation restrictions, audit event ordering, audit logger failure isolation, and interrupted atomic-write recovery.
- No production-code change was necessary: the reviewed planner templates and task state machine already implemented the required behavior. The identified gap was insufficiently precise acceptance assertions and integration coverage.
- Code/test commits: `6dd4b8072e341c502726e69d9a0591b95f547ac6` (planner assertions); `78f80cdb3977d0bf759da52b9e88c24f04d620fd` (P13–P15 processor command tests); `8a5d667f3f1c14d94a9329638ca1971711fd2500` (P16 orchestration/audit/persistence test); `1b83168cef7c507993ac2d305fde062a2b9b1eaf` (terminal-state persistence test).
- CI GitHub Actions run #263 (run ID `45114439357`) — **SUCCESS** for head `1b83168cef7c507993ac2d305fde062a2b9b1eaf`. `testDebugUnitTest` completed successfully (`BUILD SUCCESSFUL`); ARM64 native runtime and Debug APK / Android instrumentation APK builds completed and both artifacts uploaded. [Run #263](https://github.com/preecha021233-del/somchai-ai/actions/runs/45114439357).
- Debug APK artifact ID `14790644219` (ZIP size 21,825,990 bytes); Android instrumentation APK artifact ID `14791361316` (ZIP size 855,965 bytes). Instrumentation APK was built and uploaded, not executed on a physical device.
- Chunks 9–12 are DONE as work-plan deliverables. The focused P13–P16 JVM planning/task tests and full JVM unit-test suite pass. This does not claim final P01–P16 acceptance: actual Android process-restart/device evidence is still required for P16, and P01–P08 local-model quality (18/20) remains BLOCKED pending target-device model evaluation. No final Phase 1 acceptance is claimed.


## Phase 1 — Group 4 / Chunks 13–17: local memory (2026-11-23)

- Reviewed P17–P21 and the local memory store, command processor, encryption, backup/restore, and Android instrumentation coverage.
- P17: /remember returns the complete stable UUID; processor tests verify the ID can be used to recall the saved entry.
- P18: exact UUID lookup takes precedence over matching prefixes and returns the selected stored entry.
- P19: Thai content search ignores whitespace differences; regression test stores สอบวันที่ 30 and finds it with สอบวันที่30.
- P20: instrumentation coverage creates a CoreRuntime, saves memory, recreates CoreRuntime against the same app-private directory and Android Keystore key, and recalls the entry. This test was compiled into the instrumentation APK but was not executed on a physical device or after an actual process kill; therefore the device-level close/reopen acceptance remains outstanding.
- P21: deletion by exact ID is covered; ambiguous short IDs are rejected, and the full ID deletes only the matching record.
- Additional lifecycle coverage: /edit updates content while preserving ID and creation timestamp; encrypted export/import preserves exact IDs, categories and contents; tampered imports are rejected without replacing current data; encrypted backup/restore and interrupted-write recovery are covered.
- Latest code/test head: eab2c0554a9cf4671e444e39e2d04082cdee68cd (test assertion that editing preserves stable ID and timestamp).
- CI GitHub Actions run #277 (run ID 45115580134) — SUCCESS for the above head. The android-local-brain job completed successfully: JVM unit tests, ARM64 native runtime build, Debug APK build, Android instrumentation APK build, and both artifact uploads all succeeded. https://github.com/preecha021233-del/somchai-ai/actions/runs/45115580134
- Artifacts: Debug APK 14791229226 (ZIP SHA-256 fd45f9fc26f78cee9ef50b1454c9b4872f999a0e38ceb48ebe155b8eec7e959f); Android instrumentation APK 14791563072 (ZIP SHA-256 b4f5f6e6eaa129ddb3035ff13f4bf560b977b713dd8fd246dd36811bdf5ba67e).
- Chunks 13–17 are DONE as work-plan deliverables. This does not claim all P17–P21 device-level acceptance has passed: P20 still needs actual owner-device close/reopen evidence. The instrumentation APK build is not equivalent to running the test on-device.

## Phase 1 — Group 5 / Chunks 18–24: research and plugins (2026-11-23)

- Reviewed P22–P25, PluginManager, chat route policy, BuiltinToolExecutor, multi-source discovery, ResearchCoordinator, ResearchResultVerifier, WebPageReader, and their tests.
- P22 evidence: AgentApp `/plugins` status response and Plugins page report active manifests, pending candidates, and disabled manifests from separate PluginManager lifecycle buckets. Added `PluginManagerTest.reportsPendingActiveAndDisabledManifestsInTheirCorrectLifecycleBuckets` to assert all three buckets through pending → approved/active → disabled transitions.
- Added `ChatRoutePolicyTest.pluginStatusAliasesNeverRouteToWebSearchOrExternalAi`: `/plugins`, `ตรวจสอบปลั๊กอิน`, and `รายการปลั๊กอิน` route to status; `/search` and `/ai` remain distinct explicit routes.
- P23 evidence: `BuiltinToolExecutorTest.unapprovedSearchIsBlockedBeforeDiscoveryOrNetworkWork` verifies an unapproved search is rejected before discovery/provider work and before storage-budget work; `ChatOrchestratorTest.searchRequiresApproval` verifies the chat command also blocks before invoking the search callback.
- P24 implementation evidence: approved-search fake-provider test verifies two distinct page URLs are actually passed to the reader and the returned report includes both source URLs and page text. This is automated mock evidence only; the acceptance criterion requires a real query through the running app and two real pages opened/read. No live app execution has been performed, so P24 remains BLOCKED pending owner-device live test after Web Search approval.
- Page reading/extraction evidence: `WebPageReaderTest` covers visible text extraction, removal of scripts/navigation/styles, entity decoding, size limits, HTTPS-only URL validation, private/local address rejection, unsafe ports, credentials, and IPv6 special ranges. Reader resolves and pins public IP addresses before TLS connection and validates redirect destinations.
- Grounding/reliability evidence: `ResearchCoordinatorTest` covers successful reads, empty discovery, timeout/provider errors, unreadable pages, canonical duplicate URL suppression, and continuing after unreadable candidates. `ResearchResultVerifierTest` distinguishes discovery-only from actual page reads and deduplicates URLs. `BuiltinToolExecutorTest` requires source citations for summaries, reports partial results under two readable pages, and avoids logging query/URL content. `ResearchSummaryPromptTest` sanitizes and delimits retrieved page text as untrusted.
- Latest code/test commits: `2545ff8153762ce28acc6324c0c07fc93d0c5a80` (plugin lifecycle bucket test), `e659b84cafe500c6c18d46a40ea2664860e64790` (plugin status route aliases).
- CI run #279 (run ID `45119904510`) — **SUCCESS** for head `e659b84cafe500c6c18d46a40ea2664860e64790`. The `android-local-brain` job completed successfully: JVM unit tests, ARM64 native runtime build, Debug APK build, Android instrumentation APK build, and both artifact uploads all succeeded. [Run #279](https://github.com/preecha021233-del/somchai-ai/actions/runs/45119904510).
- Work chunks 18–23 and 24A are **DONE** as implementation/test deliverables. Live P24 acceptance is tracked separately as 24B and remains **BLOCKED** until a real approved search is executed in the running app and at least two distinct live source pages are confirmed read. CI success is not live-search evidence and this is not a P22–P25 acceptance PASS.

## P24 split — implementation complete, live acceptance pending (2026-11-23)

- Split the former Chunk 24 into **24A** and **24B** without changing the P24 acceptance threshold.
- **24A — DONE:** implementation-side automated coverage is complete. `BuiltinToolExecutorTest.approvedSearchReadsAndReportsActualPages` exercises approved search with two distinct page reads and source reporting; `ResearchResultVerifierTest` covers discovery-only, duplicate-source, and insufficient-readable-page states. CI #279 (run ID `45119904510`) passed on head `e659b84cafe500c6c18d46a40ea2664860e64790`. This is mock-provider/automated evidence, not a live search claim.
- **24B — BLOCKED / moved to testing:** run P24 in the actual app after the owner approves Web Search; submit a real query, confirm at least two distinct real pages were opened/read, verify the summary and traceable citations, and record app/device/commit/timestamp evidence. If fewer than two pages are readable, record PARTIAL/FAIL; do not lower the threshold.
- The 54-item tracker preserves the existing IDs for Chunks 25–53. 24B is listed in the testing/device-acceptance stage, while Chunk 53 remains the final P01–P43 decision/report.


## Phase 1 — Group 6A / Chunks 25–32: protection and recovery implementation (2026-11-23)

- Reviewed the fixed P26–P32 acceptance baseline against the production permission boundary, plugin validation/lifecycle, research prompt construction, encrypted memory store, audit logger, and HTTPS page reader. No acceptance threshold was changed.
- **P26 — permission denial:** `PermissionManagerTest` covers the allowlist, default-deny unknown capabilities, exact operation-to-permission binding, duplicate/extra permission rejection, and the search approval boundary. `PluginValidatorTest` rejects unknown permissions and permission escalation. `BuiltinToolExecutorTest.unapprovedSearchIsBlockedBeforeDiscoveryOrNetworkWork` verifies the unapproved path does not invoke discovery or the storage/network work stage.
- **P27 — plugin approval and lifecycle:** `PluginValidatorTest`, `PluginSmokeTesterTest`, and `PluginManagerTest` cover allowlisted operations, rejection of executable/arbitrary operations, disabled-plugin rejection, hash-bound owner approval, tampered-candidate rejection, active/disabled/pending buckets, disable/re-enable, and lifecycle audit events. `CoreRuntime` exposes Web Search to the executor only when the active manifest is enabled and has the expected plugin ID and operation.
- **P28 — retrieved content remains untrusted:** `PermissionManagerTest` covers English/Thai instruction-override phrases, invisible format characters, control/bidi removal and payload bounds. `ResearchSummaryPromptTest.externalPageTextIsSanitizedAndDelimitedAsUntrustedData` verifies sanitization, explicit untrusted-data delimiters and escaping before retrieved content enters the local summary prompt. The final research result is sanitized as well. These are defense-in-depth controls; they do not claim that pattern filtering alone can eliminate every possible prompt injection.
- **P29 — authenticated encrypted export:** `AesGcmMemoryCipherTest` and `LocalMemoryStoreTest.authenticatedExportImportPreservesExactIdsCategoriesAndContents` / `encryptsMemoryAndBackupThroughInjectedCipher` cover AES-GCM-backed ciphertext, authenticated export and encrypted backup behavior. Export authenticates and validates the stored payload before returning bytes.
- **P30 — restore and recovery:** `LocalMemoryStoreTest.encryptsMemoryAndBackupThroughInjectedCipher`, `restoresRollbackFileAfterInterruptedMemoryRewrite`, and `LocalMemoryStoreAuditTest.recordsMemoryBackupRestoreAndMutationLifecycleEvents` cover backup/restore, preserved records, interrupted-write recovery and audit events. The production rewrite path verifies staged ciphertext and the committed file before retiring rollback data.
- **P31 — tamper rejection:** `LocalMemoryStoreTest.rejectsTamperedImportWithoutReplacingExistingMemory`, `rejectsTamperedBackupWithoutChangingCurrentMemory`, and `rejectsDuplicateIdsInAuthenticatedMemoryImport` verify authenticated tamper/duplicate rejection without replacing existing memory.
- **P32 — unsafe URL and safe page handling:** `WebPageReaderTest` and `WebPageReaderSecurityTest` cover HTTPS-only URLs, standard port, credentials, localhost/private/reserved IPv4/IPv6, unsafe redirects, mixed public/private DNS answers, DNS-rebinding-resistant pinned TCP destination, TLS hostname verification, and extraction of visible text while dropping scripts/navigation/styles. These are automated tests; live network/device verification remains in P32B.
- **Audit evidence:** `PluginManagerTest` checks candidate-created/approved/disabled/enabled/rejected lifecycle events. `LocalMemoryStoreAuditTest` checks add/update/backup/export/clear/restore/import/delete events. The research audit test verifies query terms and full URLs are not written to the audit log.
- Code/test evidence is on the Phase 1 branch through test head `1f8508e97501767b3b7de7f81a614fed7de17add` (includes plugin lifecycle audit tests at `dfe95b994f24759d05331ad689e9b847679fa793` and memory lifecycle audit tests at `1f8508e97501767b3b7de7f81a614fed7de17add`).
- GitHub Actions run #281 (run ID `45126187843`) — **SUCCESS** for test head `1f8508e97501767b3b7de7f81a614fed7de17add`: independent JVM unit tests, ARM64 native runtime/APK build, Debug APK upload and Android instrumentation APK upload all succeeded. [Run #281](https://github.com/preecha021233-del/somchai-ai/actions/runs/45126187843). The instrumentation APK was built/uploaded; Android instrumentation was **not** executed on the owner's physical device.
- **Chunks 25–32 / Group 6A are DONE as implementation and automated-test deliverables.** Real-device checks P26B–P32B remain BLOCKED and separate. This does not claim a full P26–P32 device acceptance pass or final Phase 1 acceptance.
