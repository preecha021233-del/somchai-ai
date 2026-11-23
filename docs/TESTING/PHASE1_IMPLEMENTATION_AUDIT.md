# Phase 1 implementation audit — P01–P43

Updated: 2026-11-23  
Branch: `phase-1/local-brain-runtime`

## Decision

This is an implementation audit, not an acceptance-test result. The P01–P43 thresholds are unchanged. No case is marked PASS solely because code or a unit test exists. Phase 1 remains **NOT ACCEPTED** until the fixed suite and Android-device gates have evidence.

## P01–P08 — Internal processing and Thai understanding

| Case | Implementation location / evidence | Remaining |
|---|---|---|
| P01 greeting | `InternalProcessor`, `InternalProcessorTest` | Verify exact chat route and no network on device |
| P02 help | `InternalProcessor.helpText()`, `InternalProcessorTest` | Verify exact supported command list |
| P03–P06 arithmetic / cash change | `OfflineReasoningEngine`, `OfflineReasoningEngineTest` | Re-run unchanged prompts and verify actual output |
| P07 thunder explanation | `BasicKnowledgeEngine`, `BasicKnowledgeEngineTest` | Check model-quality rubric when model is installed |
| P08 backup pros/cons | `BasicKnowledgeEngine`, `BasicKnowledgeEngineTest` | Check model-quality rubric when model is installed |

## P09–P12 — Reasoning and verification

- Implementation: `OfflineReasoningEngine`, `ReasoningPolicy`, `ReasoningPolicyTest`, `OfflineReasoningEngineTest`.
- Includes contradiction detection, insufficient-information handling, arithmetic and no-fabricated-source fallback.
- Remaining: execute all four fixed prompts and inspect exact answers; model-quality threshold remains device-gated.

## P13–P16 — Planning and task execution

- Implementation: `TaskPlanner`, `TaskExecutionStore`, `TaskCommandProcessor`.
- Tests: `TaskPlannerTest`, `TaskExecutionStoreTest`, `TaskCommandProcessorTest`.
- Chat routing: `AgentApp` routes `/task` to the durable task command adapter and creates/starts the first task step from the plan confirmation action.
- Remaining: verify persistence after Android process/app restart and confirm no external side effect is falsely reported.

## P17–P21 — Local memory

- Implementation: `LocalMemoryStore`, Android Keystore AES-GCM cipher, encrypted export/import, relevant-memory retrieval.
- Tests: `LocalMemoryStoreTest` and encryption/export/tamper cases.
- Remaining: real-device persistence, exact export/import round-trip and tampered-file behavior with Android Keystore.

## P22–P25 — Research and plugins

- P22: `PluginManager` exposes active, pending and disabled manifests; chat has a plugin-status route.
- P23: `BuiltinToolExecutor` enforces the core permission and owner-approved Web Search plugin before discovery/network access.
- P24: `ResearchCoordinator`, `ResearchDiscoveryProvider`, `WebPageReader` distinguish discovered candidates from pages actually read; output is PARTIAL until two pages are read.
- P25: `ResearchCoordinatorTest` covers success, empty discovery, timeout, provider error, unreadable pages and continuation to later readable candidates.
- Remaining: real online device run proving two distinct pages were opened/read and references are traceable.

## P26–P32 — Protection and recovery

- Implementation: `PermissionManager`, `PluginValidator`, `PluginManager`, `WebPageReader`, encrypted memory/backup and learned-skill storage.
- Tests include `PermissionManagerTest`, `PluginManagerTest`, `LocalMemoryStoreTest`, `SkillStoreEncryptionTest`, and WebPageReader security cases.
- Remaining: run the full deny/tamper/restore suite against the current branch and verify Android Keystore behavior on device. The in-process plugin system is not claimed to be a hardened arbitrary-code sandbox.

## P33–P35 — Brain boundary

- Implementation: `LocalFirstBrain`, `NativeLlamaInferenceEngine`, and the ordinary-chat route in `AgentApp`.
- Tests: `LocalFirstBrainTest` checks local-model selection, fallback when unavailable/failing, and safe behavior if fallback also fails.
- Explicit `/ai` is a separate owner-configured optional route; ordinary chat does not route to the remote provider.
- Remaining: device/network inspection confirming ordinary chat never contacts an external AI provider and records the correct engine/fallback status.

## P36–P39 — Local model

- Implementation: `LocalModelStore`, GGUF/SHA-256 validation, JNI llama.cpp runtime and observable model lifecycle.
- Tests: `GgufFileValidatorTest` and native ARM64 build in Phase 1 CI.
- Remaining: pinned model download, native load and Thai generation on the target Android device; record latency, peak RAM, temperature and battery. CI native compilation is not device acceptance.

## P40–P43 — Learned Skills

- Implementation: `SkillStore` encrypted drafts, 2–10 test cases, explicit owner approval, matching-only retrieval and deletion revocation.
- Tests: `SkillStoreTest` and `SkillStoreEncryptionTest` cover draft-only learning, prompt-data escaping, evaluation bounds, failed-test approval denial, persistence and deletion.
- Remaining: model-backed extraction/evaluation and full approval/retrieval/audit workflow on the target device.

## New implementation work in this audit

1. Added `app/src/androidTest/java/com/somchai/ai/AgentAppInstrumentedTest.kt` to launch the real `MainActivity`, verify the chat shell, open the top-right menu and verify navigation entries.
2. Updated `phase1-local-brain.yml` to compile `assembleDebugAndroidTest` and upload the instrumentation APK. This compiles the device test but does not execute it on a device.
3. Fixed `android-ci.yml`: release identity/version/certificate verification and release artifact upload now run only when all four owner-signing secrets are configured. When configured, the certificate-continuity verification remains mandatory; missing signing secrets are reported as a warning and do not make the debug CI fail.

## Remaining implementation / verification gaps

- The new instrumentation smoke test still needs to be executed on a real Android device/emulator.
- Re-run CI after these changes; do not treat the earlier CI #139 result as verification of these commits.
- Inspect any CI failures and repair the implementation, not the acceptance thresholds.
- P36–P39 and the device-dependent portions of P01–P12, P17–P21, P22–P32 and P40–P43 remain blocked on target-device evidence.
- Owner-signed release continuity and install-over-existing-data must be verified with the protected signing secrets and the owner's existing installation baseline.


## Additional defects found and corrected — 2026-11-23

4. P02 help text did not enumerate every command exposed by the chat UI. Added `/plugins`, explicit optional `/ai`, and `/skill delete`; expanded the regression test to assert every command family and the owner-controlled external-provider boundary.
5. The Web Search route matched commands case-insensitively but removed the lowercase prefix case-sensitively. Changed query extraction to take the text after the first space, so `/SEARCH ...` and `/search ...` are handled consistently.
6. `TaskCommandProcessor` used `error()` for an unknown task and non-numeric step, producing `IllegalStateException` rather than the argument-validation contract. Changed both to `IllegalArgumentException` and added command-adapter regression coverage.

These changes are committed on the branch. Their CI result is pending; no test pass is claimed for these new commits.


## P18 memory-ID retrieval correction — 2026-11-23

7. `LocalMemoryStore.search` previously treated every ID lookup as a prefix match. An exact UUID lookup could therefore return more than one record if another ID shared that prefix. Exact full-ID matches now take precedence and return one record; short displayed IDs still work as prefixes.
8. `InternalProcessor` recognized `/RECALL` case-insensitively but removed the lowercase command prefix case-sensitively. It now removes the fixed-length command prefix, so uppercase and lowercase forms both search the intended query.
9. Added regression tests for exact-ID precedence and uppercase `/RECALL` using the stable ID displayed by `/remember`.

The code and tests are committed. CI has not yet been confirmed for these latest commits; this correction is not marked test-passed until the workflow result is available.


## Plugin disable transition hardening — 2026-11-23

10. The previous disable path copied an active manifest into the disabled-backup directory and then deleted the active copy. A failure between those operations could leave the same plugin reported as both active and disabled. The manager now prefers a same-filesystem rename for an atomic move; when rename is unavailable it writes and verifies a temporary backup before deleting the active copy. It validates the moved manifest and attempts rollback if validation fails. Existing lifecycle tests cover approve → disable → re-enable; CI for this change remains unconfirmed.


## P23 and P36 implementation completion batch — 2026-11-23

11. **P23 Web Search approval boundary:** changed the storage-budget dependency to an injected check callback so the tool boundary can be tested without Android Context. Added `BuiltinToolExecutorTest` proving an unapproved search is rejected before discovery/network work or storage-budget work; added approved-search coverage proving the tool reads and reports two actual pages and validates the query before discovery.
12. **P36 model download redirects:** removed automatic HTTP redirects. The model installer now follows at most five redirects manually, revalidates every redirect as HTTPS with no embedded credentials and standard HTTPS port, rejects local/private IP literals and local hostnames, resolves each hop and rejects non-public DNS answers, and only accepts HTTP 200. The download rejects oversized declared lengths, actual/declared size mismatches, truncated/out-of-range files, wrong SHA-256 and invalid GGUF headers before publishing the model.
13. Added unit coverage for HTTP downgrade, credentials, unsafe ports, localhost/private IP destinations, relative HTTPS redirects, wrong hash, truncated size, invalid GGUF header, content-length mismatch and insufficient storage.
14. These changes are committed on the Phase 1 branch. CI run #161 was pending at the time of this audit update; no test pass is claimed until it completes. DNS validation is defense-in-depth; device/network integration and model install/inference still require target-device evidence.


## P33–P35 chat-routing integration — 2026-11-23

15. Added a pure `ChatRoutePolicy` used by the actual `AgentApp` dispatcher. Ordinary Thai/English conversation is classified as `LOCAL_BRAIN`; only explicit `/ai <question>` is classified as `EXPLICIT_EXTERNAL_AI`. Task, skill, plugin, web-search, and internal commands each have separate routes.
16. Added `ChatRoutePolicyTest` covering ordinary chat, explicit provider opt-in, unknown slash commands, and every built-in command family. Existing `LocalFirstBrainTest` continues to cover local inference, unavailable-model fallback, runtime failure fallback and double-failure safety.
17. CI #161 failed before Kotlin unit tests or native compilation: CMake's FetchContent git clone of the pinned public `ggml-org/llama.cpp` commit returned HTTP 403 from GitHub. This is an upstream fetch/network failure, not a compiler diagnostic. The workflow was changed to run JVM unit tests as a separate first step and cache the pinned FetchContent source for later builds.
18. CI #162 was superseded by subsequent commits and may be cancelled by the workflow's concurrency policy. The current routing/test commit requires a fresh CI result; no pass is claimed.


## CI repair and verified build — 2026-11-23

19. Fixed the approved Web Search test expectation: the report correctly uses the title returned by the page reader, not the discovery candidate title. The test now asserts the two actually-read page titles, URLs, and evidence text.
20. Fixed the Android Compose instrumentation test to use the supported `assertIsDisplayed()` assertion API instead of the unavailable `assertExists()` import.
21. Verified commit: `d4a3625727fcd9caeb81efd30d8c52db8c87469e`.
22. CI run #45092099946: **SUCCESS**. JVM unit-test task passed; ARM64 llama.cpp native build passed; debug APK assembled; Android instrumentation APK compiled and assembled; both artifacts uploaded.
23. CI evidence: https://github.com/preecha021233-del/somchai-ai/actions/runs/45092099946
24. This is a CI/build pass only. The instrumentation test was compiled and packaged, not executed on a connected Android device. Device acceptance and Phase 1 remain NOT ACCEPTED.


## P16 task lifecycle audit integration — 2026-11-23

25. Added an audit callback to `TaskExecutionStore` and connected it to the app's persistent `ActivityLogger`.
26. Persisted task transitions now emit privacy-minimized events: `TASK_CREATED`, `TASK_STEP_STARTED`, `TASK_STEP_COMPLETED`, `TASK_STEP_FAILED`, `TASK_STEP_RETRY_QUEUED`, and `TASK_STEP_CANCELLED`. Event details contain task ID, step index/status and task type/count; they deliberately omit the user's task goal and failure text.
27. Audit callback failure is isolated so a logging outage cannot reverse or falsely fail an already-persisted task transition.
28. Added regression tests for the full lifecycle event sequence, cancellation, minimized event details, and audit-logger failure isolation. CI for commits `ddd763a`, `c4e89b1`, `658a024`, and `a3c4ff6` has not yet been confirmed; these tests are pending CI verification. P16 device restart evidence remains required.

## P21 safe deletion by memory ID — 2026-11-23

29. Fixed `/forget` so an exact full UUID takes precedence over shorter prefixes. If a short prefix matches multiple memory records, deletion is refused and the user is asked to supply the full ID; no arbitrary first match is deleted.
30. Added a regression test that migrates two legacy records with deliberately colliding eight-character prefixes, verifies the short ID is rejected without deleting either record, then verifies the exact full ID deletes only the intended record.
31. CI verification is pending for commits `8a5da39` and `64389bd`; no pass is claimed.

## P20 transactional memory replacement and crash recovery — 2026-11-23

32. Memory rewrites now encrypt and validate a staged candidate before touching the live file, then retain a same-directory rollback copy until the committed file is read back and validated.
33. Startup recovers the rollback file if the process stopped between renames; stale temporary files are cleaned up. A failed candidate verification removes the temporary file and preserves the original encrypted store.
34. Import and backup restore use the same transactional rewrite path. Import no longer performs a redundant post-commit read after rollback has been retired.
35. Regression tests cover candidate verification failure preserving existing data and startup recovery after an interrupted replacement. CI verification is pending.

## P16/P43 transactional task and learned-skill persistence — 2026-11-23

36. Task state writes now stage and parse the candidate JSON, preserve the previous file as a rollback copy, atomically rename the candidate, and read back the committed task records before retiring rollback. Startup restores a rollback file after an interrupted replacement.
37. Learned-skill storage now validates persisted UUIDs, names, trigger counts/lengths, procedure sizes, test/result cardinality, approval invariants, output bounds, timestamps, and duplicate IDs.
38. Learned-skill encrypted writes now decrypt and validate the staged candidate, retain rollback until read-back verification, and recover interrupted replacements on startup. Empty or oversized skill-retrieval queries return no skills.
39. Regression tests cover task-state crash recovery, encrypted-skill crash recovery, malformed persisted skill rejection, and empty-query retrieval. CI verification is pending.
