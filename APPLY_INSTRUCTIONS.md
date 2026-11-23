# SOMCHAI Phase 1 Gap-Fill — Developer Instructions

## Purpose
Integrate the Phase 1 gap-fill package without damaging the Protected Core or changing the Android application ID or signing identity.

## Scope
1. ChatOrchestrator — route local Brain, InternalProcessor, tasks, research and explicit remote AI.
2. ResearchResultVerifier — label discovery snippets separately from successfully read source pages.
3. InternalProcessor — support persisted plans and task listing without removing existing safety behavior.
4. ResultEvaluator — gate empty answers and known unsupported external side-effect claims.
5. Regression tests for routing, research evidence and result evaluation.
6. PHASE1_TEST_LOG.md — record developer and real-device acceptance evidence.

## Protected boundaries
- Do not change applicationId from com.somchai.ai.
- Do not replace or regenerate the owner signing key or keystore.
- Do not remove Android Keystore memory encryption, plugin hash-bound approval, permissions, URL validation, SSRF/private-network checks, untrusted-data sanitization, research citation validation, task audit logging or rollback behavior.
- Ordinary chat must remain local-first and must not call a remote LLM automatically.
- /search must remain blocked until the Web Search plugin is owner-approved.
- /ai may call a remote provider only after explicit owner configuration and an explicit /ai command.
- Do not claim that llama.cpp or a GGUF model is installed unless model integrity validation and native loading succeeded.
- Do not uninstall the existing app or clear app data to make an update install.
- Do not close Phase 1 without complete device acceptance evidence.

## Integration notes
- Merge InternalProcessor changes into the current implementation; retain ambiguity-safe /forget and existing reasoning checks.
- Keep BuiltinToolExecutor on the existing ResearchCoordinator and WebPageReader pipeline. Do not replace it with direct HTTP code that bypasses URL safety, approval, sanitization or citation checks.
- Keep CoreRuntime's actual LocalFirstBrain and llama.cpp setup. Do not replace it with UnavailableOnDeviceInferenceEngine.
- Route ordinary chat, /search and /ai through CoreRuntime.orchestrator. Keep skill and plugin lifecycle operations behind their existing dedicated handlers.
- Preserve all existing user data and package identity.

## Developer verification
Run:
- ./gradlew testDebugUnitTest
- ./gradlew assembleDebug
- ./gradlew assembleRelease only when the original owner signing credentials are available.

A CI run on an earlier commit does not verify later changes. Record the exact commit SHA and CI run URL.

## Device acceptance
Use docs/TESTING/PHASE1_TEST_LOG.md. Install the owner-signed update over the existing app without uninstalling or clearing data. Test offline chat, /plan, /task, memory, plugin approval/lifecycle, /search evidence labels, explicit /ai policy, response actions, model load/inference and update compatibility.

Phase 1 remains open until every required acceptance gate has evidence.
