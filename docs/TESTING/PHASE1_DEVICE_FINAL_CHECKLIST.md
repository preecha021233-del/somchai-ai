# Phase 1 — Final on-device acceptance checklist

Record one row per test. Do not mark PASS without observable evidence. Use BLOCKED when a prerequisite is unavailable; do not reinterpret BLOCKED as PASS.

**Run metadata**
- Date/time:
- Device/model:
- Android version:
- ABI:
- App versionName/versionCode:
- APK SHA-256:
- Signing certificate SHA-256:
- Repository commit:
- Tester:

For every item, record **Result: PASS / FAIL / BLOCKED**, **Evidence** (screenshot, sanitized log, command output, or test artifact), and **Notes**.

## Set A — Core functions and update compatibility

| ID | Test | Result | Evidence | Notes |
|---|---|---|---|---|
| A01 | Verify package ID and signing certificate; install owner-signed Release over existing app with adb install -r only | BLOCKED | | Never uninstall or clear data |
| A02 | Verify existing chats, memories and app settings remain after update | BLOCKED | | Compare before/after |
| A03 | Disable network; send Thai greeting and basic capability question | BLOCKED | | Confirm local/internal response; no remote request |
| A04 | Disable network; ask a basic reasoning/calculation question and inspect honesty/fallback | BLOCKED | | Record exact prompt and output |
| A05 | Create plan using /plan ... | BLOCKED | | Record plan and expected steps |
| A06 | Create task using /task ...; list/show/start/complete/fail/retry/cancel supported lifecycle | BLOCKED | | Confirm state changes persist |
| A07 | Test /remember, /recall, and /forget including ambiguous short-ID refusal | BLOCKED | | Verify only intended memory is deleted |
| A08 | Run memory backup and owner-confirmed restore | BLOCKED | | Verify restored content and confirmation |
| A09 | Run encrypted export/import round trip on same installation/key | BLOCKED | | Not a cross-device migration format |
| A10 | Plugin candidate approve and reject | BLOCKED | | Verify owner confirmation and audit events |
| A11 | Plugin disable and enable | BLOCKED | | Verify active state changes |
| A12 | Plugin rollback after failed/invalid candidate | BLOCKED | | Verify previous approved state restored |

## Set B — Research and permissions

| ID | Test | Result | Evidence | Notes |
|---|---|---|---|---|
| B01 | Before Web Search plugin approval, run /search SOMCHAI security test | BLOCKED | | Must be blocked by orchestrator and tool boundary |
| B02 | Approve Web Search plugin, then run a real /search query | BLOCKED | | Record exact query and sanitized output |
| B03 | Verify URLs discovered versus pages actually opened/read | BLOCKED | | Discovery snippets are not read-page evidence |
| B04 | Verify evidence level READ_MULTI_SOURCE, READ_SINGLE_SOURCE, DISCOVERY_ONLY, or NONE matches actual pages read | BLOCKED | | Two distinct read pages required for multi-source |
| B05 | Verify cited source IDs correspond only to pages actually read | BLOCKED | | Reject invented or out-of-range citations |
| B06 | Run /ai without configured provider credentials | BLOCKED | | Must be blocked; ordinary chat must not call remote AI |
| B07 | Configure provider only if owner explicitly chooses, then invoke /ai explicitly | BLOCKED | | Not a default chat route |

## Set C — Network security / DNS rebinding

| ID | Test | Result | Evidence | Notes |
|---|---|---|---|---|
| C01 | Attempt HTTPS read of localhost and loopback targets | BLOCKED | | Must fail before request reaches local service |
| C02 | Attempt HTTPS read of RFC1918 private targets (10/8, 172.16/12, 192.168/16) | BLOCKED | | Must be rejected |
| C03 | Attempt HTTPS read of link-local/cloud metadata 169.254.169.254 | BLOCKED | | Must be rejected |
| C04 | Attempt .local, .internal, .lan hostnames | BLOCKED | | Must be rejected |
| C05 | Attempt http:// URL | BLOCKED | | HTTPS only |
| C06 | Attempt URL with userinfo and non-443 port | BLOCKED | | Both rejected |
| C07 | Redirect from public HTTPS host to localhost/private IP | BLOCKED | | Redirect destination independently validated and pinned |
| C08 | Controlled DNS-rebinding test: first DNS answer public, subsequent answer private | BLOCKED | | TCP destination remains exactly the first validated IP; no second hostname resolution |
| C09 | Verify TLS SNI and certificate hostname use original URL hostname while TCP peer is pinned IP | BLOCKED | | Capture safe test-server evidence |
| C10 | Verify no private response content, metadata, credentials or internal addresses leak into chat | BLOCKED | | Record sanitized output only |
| C11 | Verify redirects are not automatically followed by an HTTP library | BLOCKED | | Reader handles each redirect itself |
| C12 | Verify timeouts, response byte cap, header cap and HTML/XHTML/plain-text content-type allowlist | BLOCKED | | Oversize/unsupported content must fail safely |


## Set D — Local model installation, inference and Thai quality

| ID | Test | Result | Evidence | Notes |
|---|---|---|---|---|
| D01 | From Settings, download and install the pinned Qwen2.5 0.5B GGUF model | BLOCKED | | Record progress, final status, file size and SHA-256 verification; do not sideload an unverified model |
| D02 | Force-stop and relaunch the app without clearing data; verify the installed model is revalidated and loaded off the main thread | BLOCKED | | Record cold-start/load time and model status |
| D03 | Disable network and run ordinary Thai chat; verify local engine status and no automatic external-provider request | BLOCKED | | Pair with B06/B07 and 35B network evidence |
| D04 | Run the unchanged P01–P12 Thai quality rubric using exact prompts and scoring rules | BLOCKED | | Required threshold remains at least 18/20; attach exact prompts, outputs and scoring |
| D05 | Measure first-token/total response latency, peak RAM, temperature and battery impact during the fixed prompt set | BLOCKED | | Record device model, Android build, ambient conditions and test duration |
| D06 | Exercise long Thai input/context trimming and verify an honest bounded response or clear limit error | BLOCKED | | Do not claim model quality based on short greeting only |

## Set E — Learned skills on the running app

| ID | Test | Result | Evidence | Notes |
|---|---|---|---|---|
| E01 | Use `/skill learn` with a real successful experience and at least two distinct test cases | BLOCKED | | Must create a draft only; no automatic activation |
| E02 | Run `/skill test <id>` with passing and failing cases | BLOCKED | | Record each case; any failed case must prevent approval |
| E03 | Explicitly approve a skill only after every test passes; verify matching trigger retrieves it in ordinary local chat | BLOCKED | | Confirm unrelated prompts do not retrieve the skill |
| E04 | Delete the approved skill and verify it is no longer retrieved; inspect lifecycle audit events | BLOCKED | | Do not include private procedure/test payloads in audit output |
| E05 | Restart the app and verify encrypted skill persistence, approval state and retrieval boundary | BLOCKED | | Same app installation/Keystore key; do not clear data |

## Set F — Security acceptance mapping for P26B–P32B

| ID | Test | Result | Evidence | Notes |
|---|---|---|---|---|
| F01 / P26B | Attempt an operation without its declared permission and verify denial plus expected audit event | BLOCKED | | Use a harmless controlled test operation |
| F02 / P27B | Verify pending, rejected and disabled plugins cannot execute; verify only the approved active manifest is executable | BLOCKED | | Use a harmless test plugin; no arbitrary downloaded code |
| F03 / P28B | Read a benign controlled page containing prompt-injection instructions and verify those instructions remain untrusted data | BLOCKED | | Do not expose secrets or permit side effects |
| F04 / P29B–P30B | Export a real encrypted backup, restore a known-good copy and verify exact record count, IDs and contents | BLOCKED | | Keep an untouched backup copy; never overwrite the only valid backup |
| F05 / P31B | Tamper with a duplicate test backup and verify import rejection without changing existing memory | BLOCKED | | Never tamper with the only valid backup |
| F06 / P32B | Run safe HTTPS and controlled unsafe URL/redirect cases; verify only safe visible text is read | BLOCKED | | Use controlled endpoints and capture sanitized evidence |

## Blocked work mapping

- **24B:** B02–B05 — live approved research, at least two distinct pages actually read, correct evidence level and citations.
- **35B:** A03, B06–B07, D03 — prove ordinary chat stays local and only explicit `/ai` can contact the configured provider.
- **40–41:** D01–D06 — real model installation, inference, Thai quality and resource measurements.
- **47B:** E01–E05 — model-backed skill lifecycle in the running app.
- **P26B–P32B:** F01–F06, with C01–C12 for network/URL/DNS protections.
- **52:** A01–A02 — owner-signed in-place update over the existing app and data-retention verification. Never uninstall or clear data.

## Acceptance summary

- Set A: ___ PASS / ___ FAIL / ___ BLOCKED
- Set B: ___ PASS / ___ FAIL / ___ BLOCKED
- Set C: ___ PASS / ___ FAIL / ___ BLOCKED
- Open defects:
- Evidence folder/link:
- Reviewer:
- Decision: ACCEPT / REJECT / INCOMPLETE

Phase 1 is not complete merely because CI passes. Real-device checks, model inference/Thai quality, update compatibility, security and all Phase 1 P01–P43 evidence remain mandatory.
