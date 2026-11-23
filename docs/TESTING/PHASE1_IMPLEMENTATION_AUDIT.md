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
