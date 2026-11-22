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
