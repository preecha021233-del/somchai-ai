# SOMCHAI AI — Phase 1 Test Log

This log is the evidence register for evaluating SOMCHAI AI capabilities phase by phase. Keep observed behavior separate from code-level tests and CI results. Never mark a capability passed solely because code was added.

## Round 1 — Installed app baseline (2026-11-22)

**Purpose:** Compare the installed app's actual behavior with the Phase 1 goals before the next internal-processing upgrade.

**Environment / build:** Android app package `com.somchai.ai`; installed build reported in the project update as v1.6.0 / versionCode 7. The test was performed by the project owner on an Android device. Exact device OS/build and test APK checksum were not recorded.

### Results

| Area | Test / observation | Result | Evidence / limitation |
|---|---|---|---|
| Greeting | Sent a Thai greeting | PASS (basic) | Returned a deterministic greeting. |
| Help / command discovery | Used `/help` | PASS (basic) | Listed supported commands. |
| Unknown Thai question | Asked an open-ended question outside known rules | FAIL | Returned the generic unsupported-rule response; no independent reasoning. |
| Task Planner | Used `/plan` for several materially different tasks | PARTIAL / FAIL | Returned essentially the same six-step template; did not tailor a plan to the task or execute it. |
| Local memory write | Used `/remember` | PASS | Saved a local entry and returned a short ID. |
| Local memory read by text | Used `/recall` with a phrase | PARTIAL | Returned matching notes, but also unrelated feedback entries; filtering was too broad. |
| Local memory read by ID | Used `/recall` with short and full IDs | FAIL | Reported no matching entry. |
| Local memory delete | Used `/forget` with an ID | PASS | Deleted the selected entry; subsequent recall no longer showed it. |
| Memory persistence | Closed and reopened the app, then recalled a saved note | PASS | Saved note remained available after restart. |
| Web Search permission gate | Used `/search` before approving Web Search | PASS (safety gate) | Correctly refused to search until the plugin was approved. No live search was tested. |
| Plugin inspection via natural language | Asked SOMCHAI to list/check installed plugins | FAIL / unsupported | Fell through to generic unsupported response or a generic plan; no actual plugin inventory was shown. |
| Open-ended explanation / research | Asked why the sky is blue and requested research via `/plan` | FAIL | No explanation or research was performed; generic unsupported response / generic plan. |
| Chat UI visual match | Compared installed UI with the supplied Figma target | FAIL (deferred) | Owner reported it was not close to the reference. Owner explicitly deferred this UI work; do not treat as passed. |

### Round 1 assessment

- **Working at a basic level:** deterministic greetings/help, local memory persistence/write/delete, and plugin permission gating.
- **Partially working:** text recall and task planning.
- **Not meeting the intended capability yet:** open-ended Thai intent/context understanding, task-specific planning, independent reasoning, autonomous research/fact checking, and natural-language plugin inspection.
- **Important distinction:** command handlers and templates are not evidence of a general reasoning engine.
- The installed baseline tests above were owner-observed manual tests, not automated unit tests. The detailed prompts and outputs are summarized from the test conversation; raw device logs/screenshots were not archived.

## Round 2 — Offline processing upgrade (PR #11; pending validation at log creation)

**Change under evaluation:** bounded deterministic Thai intent classification, arithmetic expression evaluation, supported word-problem solvers, and stricter local-memory retrieval / ID lookup.

**Code-level regression tests added:** greeting, capabilities, supported fact question, honest unknown response, task planning, local memory write/read, purchase-change calculation, arithmetic precedence/parentheses, Thai example classification, memory ID-prefix/full-ID lookup, and all-terms memory search.

**Status at log creation:** PR #11 is open and GitHub Actions unit tests/build are still running. These code-level tests are NOT yet recorded as passed. No Round 2 APK has been installed or manually tested on the owner's device. Do not infer a device pass from a successful CI run.

PR: https://github.com/preecha021233-del/somchai-ai/pull/11
CI run: https://github.com/preecha021233-del/somchai-ai/actions/runs/45025147721

### Required Round 2 manual checks (after CI succeeds and the signed APK is available)

1. Confirm package remains `com.somchai.ai`, update installs over the existing app, and prior local memory remains intact.
2. Test Thai intent classification using a numbered set containing a question, command, and advice request; check both labels and explanations.
3. Test arithmetic with parentheses and operator precedence.
4. Test the purchase-change word problem and verify the arithmetic independently.
5. Test memory recall by exact text, short ID, full ID, and a multi-term query; confirm unrelated entries are excluded.
6. Re-run unknown-question and open-ended research prompts; ensure unsupported claims remain honest.
7. Re-test offline behavior and verify no unapproved external AI/network call is made.
8. Record exact prompt, exact output, build/version, result, and evidence for every check.

## Ongoing recording rules

For every subsequent round and every phase:
- Add a dated round heading; do not overwrite earlier results.
- Record app version/versionCode, commit SHA, device/OS when available, and test method (manual device, automated unit, instrumentation, CI).
- Record exact prompt/input and observed output, expected behavior, actual behavior, PASS / PARTIAL / FAIL / BLOCKED, and evidence link or artifact.
- Keep manual device tests separate from automated tests and CI.
- A feature is phase-accepted only when its phase acceptance criteria are met with evidence; code merged or CI green alone is insufficient.
- Record regressions as new entries and link the earlier passing round.
- Never claim a test was run or passed without evidence.

## Round 2 — 2026-11-22 — Arithmetic and Thai memory retrieval hardening

- Phase: Phase 1
- App version / versionCode: 1.7.0 / 8 (source branch; not yet installed on device)
- Commit range: `4a0e08707ecc8c4ebb8cc1a77627966be20a3cce` through `c30127b97f8e08d05be898136ac88f012fd90993`
- Test method: automated unit tests in GitHub Actions; no manual device test yet
- CI run: https://github.com/preecha021233-del/somchai-ai/actions/runs/45026776652
- CI status at log update: in progress; no pass/fail conclusion recorded yet

### Changes under test
- Arithmetic parser now checks that every character in the selected expression is tokenized; unsupported characters cannot be silently skipped.
- Unary plus/minus are supported, including a sign before a parenthesized expression.
- Regression cases added for precedence, nested parentheses, negative values, decimals, and malformed expressions.
- Local memory search now compares a whitespace-compacted Thai query against whitespace-compacted content/category before its all-term fallback.
- Memory ID prefix search trims surrounding whitespace.

### Automated test cases added
- 8 arithmetic expressions: `2 + 3 * 4`, `(2 + 3) * 4`, `10 - (2 + 3) * 2`, nested parentheses, nested multiplication, unary negative, negative parenthesized factor, and decimal unary plus.
- 1 malformed arithmetic expression must be rejected rather than returning a partial result.
- 1 Thai memory query without spaces and 1 whitespace-padded ID lookup.

### Results
- Unit tests: PENDING CI
- Debug APK build: PENDING CI
- Owner-signed APK and signing continuity: PENDING CI
- Manual device tests: NOT RUN
- Phase 1 acceptance: NOT ACCEPTED

### Follow-up
- Do not merge or distribute this revision until CI completes successfully.
- After a successful build, install only as an in-place update and repeat device tests.
- Continue with offline Brain/model benchmarking, task execution planning, plugin runtime, permission matrix, and backup/restore acceptance tests.



## Round 3 — 2026-11-22 — Local Brain routing and safe web page reader

- Phase: Phase 1
- App version / versionCode: 1.8.0 / 9
- Commit: 3d20e0be7aebd0b15b4792385f8724eab8b53f32
- Pull request: https://github.com/preecha021233-del/somchai-ai/pull/13
- Test method: GitHub Actions CI on Ubuntu / Java 17 / Android SDK 35
- CI run: https://github.com/preecha021233-del/somchai-ai/actions/runs/45032186488

### Automated results

| Check | Result | Evidence |
|---|---|---|
| Gradle testDebugUnitTest | PASS | CI step completed successfully |
| assembleDebug | PASS | CI step completed successfully |
| assembleRelease | PASS | Owner-signed release artifact produced |
| Package identity | PASS | Verified package ID com.somchai.ai |
| Version increment | PASS | versionCode 9 exceeds the configured owner-signed baseline |
| Signing certificate continuity | PASS | SHA-256 signing certificate matched the owner-signed baseline |

New WebPageReader unit tests cover visible-text extraction, script/navigation removal, numeric HTML entities, output bounds, HTTPS URL acceptance, and rejection of HTTP, localhost, private IP, unsafe port, embedded credentials and private host suffixes.

Artifacts:
- Debug APK artifact ID: 14758471792; ZIP SHA-256: 922bf4856baa0b29e5ffd22adbefed46e657a1bf1f713f318867e134048ac1f5
- Owner-signed release APK artifact ID: 14758451820; ZIP SHA-256: 4246ad1c9ad0ac48f1634ee1cfa4911452c81cfe544b3d81569049a3b6383e91

### Not tested / not passed

- Live search and actual page-reading success on the owner's Android device.
- Whether two independent source pages are readable for a real query.
- Local language model installation, Thai model quality, latency, RAM, heat and battery benchmark.
- Manual UI, response actions, memory persistence/export/restore, plugin lifecycle and fault-recovery tests on the target device.
- In-place installation of version 1.8.0 over the owner's current installed app. Do not uninstall or clear data to work around any failure.

CI success is not a Phase 1 completion decision. The above device/model checks remain open.


## Round 4 — 2026-11-22 — Native local Brain runtime implementation

- App version / versionCode: 1.9.0 / 10 (development branch; not installed on device)

- Phase: Phase 1
- Branch: `phase-1/local-brain-runtime`
- Starting point: `phase-1/research-reader-brain-integration`
- Test method: source review; native CI and real-device tests pending
- Native engine: llama.cpp commit `8d60873dfb8619f2b5ed31adfbab07cccb40db5c`
- Starter model: Qwen2.5-0.5B-Instruct-GGUF Q4_K_M
- Model SHA-256: `74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db`

### Changes added
- JNI/CMake integration for real llama.cpp model loading and token generation.
- Native inference adapter connected to LocalFirstBrain.
- Explicit in-app download/install control for the 491 MB official Qwen GGUF model.
- Model installer checks free storage, HTTPS response, bounded size, GGUF header and SHA-256 before installation.
- Model stored in app-private storage; ordinary chat remains independent from the optional external provider.
- Added GGUF file-validator and reasoning-policy JVM tests, plus a dedicated ARM64 native CI workflow.
- Added a Thai reasoning policy for intent/context, assumption, contradiction, missing-information and source-integrity checks. The policy is prompt guidance; answer quality remains unverified.
- Expanded online discovery to DuckDuckGo HTML Web Search and added HTTPS result-link parser tests; research attempts to open up to three real pages. Live on-device multi-source results remain untested.

### Results
- Source changes: ADDED; not yet accepted.
- Initial CI snapshot at commit `7f44bf85134de0a346aaa703fe0502c51e0c003f`: PASS — run #45035557468; JVM tests, native CMake/NDK ARM64 build and debug APK completed successfully.
- Initial debug artifact: ID `14758979984`; ZIP SHA-256 `16b0dc071d541c5c8807b874e6ee5672aa71ec334a67f2b8cb6257151bcf7a45`.
- Later branch updates added the reasoning-policy tests and stronger installed-model integrity checks after that CI snapshot; those latest changes still require a fresh CI run.
- Model download and checksum verification on Android: NOT RUN.
- Native model load and Thai generation on target device: NOT RUN.
- RAM, latency, heat and battery benchmark: NOT RUN.
- Phase 1 acceptance: NOT ACCEPTED.

### Follow-up
- Fix all compile/native-link/runtime failures reported by CI.
- Install the model on the target device and run P01–P12 plus P36–P39.
- Do not claim that the starter 0.5B model has advanced reasoning ability without benchmark evidence.
- Preserve the existing app ID, owner signing key and app data; do not ask the owner to uninstall or clear data.


## Round 5 — 2026-11-22 — Native Brain and learned-skill integration CI

- Branch: `phase-1/local-brain-runtime`
- Commit tested: `2019246a51e5453edfdcc4b49b21ced930b57eac`
- App version / versionCode: 1.9.0 / 10
- CI run: https://github.com/preecha021233-del/somchai-ai/actions/runs/45036781218
- Test method: GitHub Actions, Ubuntu, Java 17, Android SDK/NDK, ARM64 native CMake build

### Automated results

| Check | Result | Evidence |
|---|---|---|
| Gradle JVM unit tests | PASS | `testDebugUnitTest` completed |
| Kotlin compile | PASS | `compileDebugKotlin` completed |
| llama.cpp native ARM64 build/link | PASS | workflow step "Run JVM tests and build ARM64 APK with native llama.cpp" completed |
| Android debug APK | PASS | `assembleDebug` and artifact upload completed |
| Learned-skill tests | PASS | Draft from experience, no auto-approval, failed-test approval block, persistence, retrieval and deletion revocation included in JVM suite |
| Model hash constant | PASS — source reviewed against official Hugging Face file metadata | Official model file page lists SHA-256 `74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db` |

- Debug artifact ID: `14760117769`
- ZIP SHA-256: `0a03f6b1a8d687c524d88394308aedf7f459053a986705eb1839233c4fca13de`

### Still not tested / not passed

- Downloading the 491 MB model on the owner's Android device.
- Native GGUF load and Thai generation on the target device.
- P01–P12 model quality threshold (18/20 and reasoning 4/4).
- Real-world RAM, latency, heat and battery measurements.
- Live research verification that at least two distinct source pages can be opened and read on device.
- Full owner-signed release APK certificate continuity and in-place update over the currently installed app.
- Remaining Phase 1 device, plugin lifecycle, backup/recovery and security acceptance checks.

### Decision

- This commit passes the automated JVM + ARM64 native build gate.
- Phase 1 remains IN PROGRESS and is NOT ACCEPTED until all fixed acceptance criteria, including target-device model and update checks, pass.


## Round 6 — 2026-11-22 — Learned-skill AES-GCM encryption and tamper rejection

- Phase: Phase 1
- Branch: `phase-1/local-brain-runtime`
- Commit: `50e6a8be9c1d5f64218ae318ae85da31349889de`
- CI run: https://github.com/preecha021233-del/somchai-ai/actions/runs/45037203591
- Test method: GitHub Actions JVM tests + Android ARM64 native build + debug APK assembly
- CI result: **PASS**. The `android-local-brain` job completed successfully; its JVM test/native build/APK build step and debug APK upload step both passed.

### Added automated security checks
- Verify an encrypted learned-skill store does not expose the skill name or procedure as plaintext.
- Verify the skill can be retrieved after encrypted persistence.
- Modify the ciphertext and verify AES-GCM authentication rejects the tampered data.

### Evidence boundary
- This is automated code-level evidence only. It does not prove the model can reliably learn useful skills, that semantic test cases are strong, or that the feature works on the owner's device.
- Device testing, full Phase 1 acceptance criteria P01–P35, and model-quality thresholds remain open.


## Round 7 — 2026-11-22 — Permission, plugin, planner and Brain-context hardening

- Phase: Phase 1
- Branch: `phase-1/local-brain-runtime`
- Code commit under test: `6d4569de1bb33f296ade3a2e5a2c5a94e7276942`
- Documentation commit: `4c4a836949f30610df262b93e3437c7e19c4c53b`
- CI run: https://github.com/preecha021233-del/somchai-ai/actions/runs/45042766846
- Test method: GitHub Actions JVM unit tests + Kotlin compile + llama.cpp ARM64 native build + debug APK assembly
- CI status at log update: IN PROGRESS; no pass/fail result claimed.

### Implementation and regression coverage added
- Exact operation-to-permission binding; reject unknown operations, duplicate capabilities and permission escalation.
- Strip control/bidirectional formatting characters and neutralize common Thai/English prompt-injection phrases in external web text.
- Ignore disabled plugin manifests at runtime and reject disabled manifests during approval/reactivation.
- Reject oversized/control-character task goals rather than silently truncating them; recognize additional Thai research-intent phrases.
- Escape saved-memory content inside an explicit untrusted-data prompt boundary and keep its closing delimiter within the prompt budget.
- Return a safe fallback if both local inference and deterministic fallback fail.
- Added JVM regression cases for each behavior.

### Evidence boundary / remaining checks
- New JVM and native CI result: PENDING at time of recording.
- Target-device model download, native load, Thai answer quality, RAM/latency/thermal/battery measurements: NOT RUN.
- Live research requiring at least two actually read pages: NOT RUN on device.
- Owner signing-certificate continuity and in-place update: NOT RUN.
- Phase 1: NOT ACCEPTED.
