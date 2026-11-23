# SOMCHAI AI — Phase 1 Work Plan (53 Chunks)

**Branch:** `phase-1/local-brain-runtime`  
**Acceptance baseline:** `docs/TESTING/PHASE1_ACCEPTANCE.md` (P01–P43)  
**Purpose:** Track the remaining Phase 1 work in one canonical table. Update this file as each chunk is completed so completed work is not needlessly repeated.

## Tracking rules

- There are 53 planned chunks; each is planned for no more than 30 minutes of focused work.
- The 26h 30m total is a planning baseline, not a guaranteed elapsed-time estimate.
- Status values: `NOT_STARTED`, `IN_PROGRESS`, `DONE`, `BLOCKED`.
- Mark a chunk `DONE` only after its deliverable is complete and its evidence is recorded (commit, test result, device result, or documented finding).
- A chunk may conclude that existing implementation already satisfies it; in that case record the exact existing evidence and mark it `DONE` without repeating the work.
- Once marked `DONE`, do not re-run or re-check it without a relevant code/requirement change, failed evidence, security concern, or final acceptance need. If reopened, explain why in Notes.
- Update this same file when a chunk finishes. Batch multiple completed rows into one GitHub update when several chunks finish together; do not create duplicate tracking files.
- Device-owner actions are required for chunks 41 and 52. Keep them `BLOCKED` until the required device evidence is available; do not claim completion on the basis of CI alone.
- This tracker does not authorize merging PRs, changing signing credentials, uninstalling the app, or clearing user data.
- Initial statuses are `NOT_STARTED`; they track completion of this plan, not whether related code already exists.

## Progress summary

| Metric | Count |
|---|---:|
| Total chunks | 53 |
| Done | 3 |
| In progress | 0 |
| Blocked | 0 |
| Not started | 50 |

## Work tracker

| # | Group | Criteria | Work / completion deliverable | Status | Completed (date) | Evidence (commit / test / report) | Notes |
|---:|---|---|---|---|---|---|---|
| 1 | Internal brain | P01–P08 | Compare Thai understanding criteria with current implementation and existing evidence; record only genuine gaps | DONE | 2026-11-23 | `PHASE1_TEST_LOG.md` — Phase 1 / Chunk 1 gap assessment; source/test SHA references recorded there | Code/test review complete. P01/P03–P08 have matching implementation/tests; P02 exact-help-list assertion is incomplete. Local-model 18/20 rubric remains BLOCKED pending model/device evidence. |
| 2 | Internal brain | P01–P08 | Fill verified Thai language-understanding gaps | DONE | 2026-11-23 | `dcfa1da821fc4e7dc3acfc1e899f205a51bfc3df`; CI run #240 SUCCESS | Exact ordered `/help` command-list assertion passes in CI. This closes the Chunk 2 test-coverage deliverable only; it does not accept P01–P08 or the local-model 18/20 gate. |
| 3 | Internal brain | P01–P08 | Improve intent and context extraction where evidence shows gaps | DONE | 2026-11-23 | b7b196848971392400e0db21769abf7f838fcb41; tests 11c68bec872a4545d38a6876cc2e8ad4474ac2d3; CI #243 SUCCESS | Question-vs-command precedence and explicit-negation guard regression tests pass in CI. This closes Chunk 3 only; it does not establish all P01–P08 acceptance criteria. |
| 4 | Internal brain | P01–P08 | Handle ambiguous and incomplete questions appropriately | NOT_STARTED | — | — | |
| 5 | Reasoning | P09–P12 | Improve fact analysis for identified failing cases | NOT_STARTED | — | — | |
| 6 | Reasoning | P09–P12 | Improve contradiction detection for identified failing cases | NOT_STARTED | — | — | |
| 7 | Reasoning | P09–P12 | Improve reasoning and explanation quality against acceptance cases | NOT_STARTED | — | — | |
| 8 | Reasoning | P09–P12 | Add/complete answer correctness checks before responding | NOT_STARTED | — | — | |
| 9 | Planning | P13–P16 | Improve multi-step planning for verified gaps | NOT_STARTED | — | — | |
| 10 | Planning | P13–P16 | Improve task decomposition and prioritization | NOT_STARTED | — | — | |
| 11 | Planning | P13–P16 | Complete task status tracking and failure handling | NOT_STARTED | — | — | |
| 12 | Internal brain | P01–P16 | Close only remaining gaps and run focused acceptance tests for P01–P16 | NOT_STARTED | — | — | |
| 13 | Memory | P17–P21 | Review memory save/recall behavior against existing evidence | NOT_STARTED | — | — | |
| 14 | Memory | P17–P21 | Complete relevance-based memory search/selection where needed | NOT_STARTED | — | — | |
| 15 | Memory | P17–P21 | Complete memory edit/delete lifecycle where needed | NOT_STARTED | — | — | |
| 16 | Memory | P17–P21 | Complete encryption, backup, and restore behavior where needed | NOT_STARTED | — | — | |
| 17 | Memory | P17–P21 | Test memory lifecycle and close verified gaps | NOT_STARTED | — | — | |
| 18 | Research | P22–P25 | Review internet search behavior against acceptance evidence | NOT_STARTED | — | — | |
| 19 | Research | P22–P25 | Complete source selection and filtering where needed | NOT_STARTED | — | — | |
| 20 | Research | P22–P25 | Complete actual webpage reading behavior | NOT_STARTED | — | — | |
| 21 | Research | P22–P25 | Improve extraction of page content and key information | NOT_STARTED | — | — | |
| 22 | Research | P22–P25 | Ensure summaries are grounded in retrieved sources | NOT_STARTED | — | — | |
| 23 | Research | P22–P25 | Handle source reliability, insufficient evidence, and partial results | NOT_STARTED | — | — | |
| 24 | Research | P22–P25 | Verify search and reading of at least two distinct pages with evidence | NOT_STARTED | — | — | |
| 25 | Security | P26–P32 | Complete permission and owner-approval enforcement | NOT_STARTED | — | — | |
| 26 | Security | P26–P32 | Complete plugin validation and integrity checks | NOT_STARTED | — | — | |
| 27 | Security | P26–P32 | Close verified command and file-safety gaps | NOT_STARTED | — | — | |
| 28 | Security | P26–P32 | Close verified network-security gaps | NOT_STARTED | — | — | |
| 29 | Security | P26–P32 | Complete audit-log coverage for relevant lifecycle actions | NOT_STARTED | — | — | |
| 30 | Security | P26–P32 | Complete backup and rollback behavior for supported operations | NOT_STARTED | — | — | |
| 31 | Security | P26–P32 | Fix security findings and run targeted regression tests | NOT_STARTED | — | — | |
| 32 | Security | P26–P32 | Record evidence for P26–P32 security acceptance cases | NOT_STARTED | — | — | |
| 33 | Brain routing | P33–P35 | Complete internal-brain routing behavior | NOT_STARTED | — | — | |
| 34 | Brain routing | P33–P35 | Complete safe fallback behavior and user-facing status | NOT_STARTED | — | — | |
| 35 | Brain routing | P33–P35 | Verify external AI is never used automatically as the primary brain | NOT_STARTED | — | — | |
| 36 | On-device model | P36–P39 | Verify local model file discovery and GGUF loading path | NOT_STARTED | — | — | |
| 37 | On-device model | P36–P39 | Verify SHA-256/model integrity enforcement | NOT_STARTED | — | — | |
| 38 | On-device model | P36–P39 | Complete native runtime integration on supported Android ABI | NOT_STARTED | — | — | |
| 39 | On-device model | P36–P39 | Complete memory/resource handling and inference error handling | NOT_STARTED | — | — | |
| 40 | On-device model | P36–P39 | Validate Thai response quality and performance on target Android hardware | NOT_STARTED | — | — | |
| 41 | On-device model | P36–P39 | Run real-device model load/inference checks and record owner-device evidence | NOT_STARTED | — | — | Requires owner device; CI is not a substitute |
| 42 | Learned skills | P40–P43 | Complete extraction of candidate skills from approved experience | NOT_STARTED | — | — | |
| 43 | Learned skills | P40–P43 | Complete skill test-case creation and minimum test coverage | NOT_STARTED | — | — | |
| 44 | Learned skills | P40–P43 | Complete skill evaluation and rejection of unsafe/weak candidates | NOT_STARTED | — | — | |
| 45 | Learned skills | P40–P43 | Enforce explicit owner approval before a skill becomes active | NOT_STARTED | — | — | |
| 46 | Learned skills | P40–P43 | Complete context-matched retrieval of approved skills | NOT_STARTED | — | — | |
| 47 | Learned skills | P40–P43 | Verify skill lifecycle, audit history, and acceptance evidence | NOT_STARTED | — | — | |
| 48 | Integration | P01–P43 | Resolve cross-system integration defects found during implementation | NOT_STARTED | — | — | |
| 49 | Integration | P01–P43 | Run focused and integration acceptance tests across P01–P43 | NOT_STARTED | — | — | Reuse valid existing evidence; do not repeat unaffected checks |
| 50 | Integration | P01–P43 | Fix integration-test failures and record targeted regression evidence | NOT_STARTED | — | — | |
| 51 | Build | P01–P43 | Build and verify the intended APK artifact and record hashes/build evidence | NOT_STARTED | — | — | Owner signing credentials remain private |
| 52 | Device acceptance | P01–P43 | Install/update in place and verify data retention plus device checklist without uninstall/clear-data | NOT_STARTED | — | — | Requires owner device and owner action |
| 53 | Final acceptance | P01–P43 | Publish final P01–P43 evidence report and state Phase 1 outcome accurately | NOT_STARTED | — | — | Do not declare PASS with missing device evidence |

## Completion policy

Phase 1 is not considered accepted merely because CI passes. The final report must map every P01–P43 criterion to evidence and clearly identify PASS, FAIL, or BLOCKED. Any unmet criterion remains visible; do not lower acceptance requirements to force a pass.
