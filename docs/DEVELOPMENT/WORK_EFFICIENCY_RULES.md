# Mandatory Development Workflow Rules

These rules are mandatory for all SOMCHAI AI development work, including Phase 1.

## 1. Do not repeat checks without a reason
- Once a file, component, requirement, or test has been checked and its result recorded, do not repeat the same check unless relevant code, configuration, dependencies, environment, or requirements have changed.
- Treat a passing result as valid for the exact commit and environment in which it was produced. Do not claim it proves a different commit or a real-device environment.
- Do not re-inspect the entire repository or re-read unchanged files for every work item.

## 2. Record evidence and reuse it
- Record what was checked, the result, the commit SHA, the test command or method, and the environment.
- Reuse recorded evidence when no relevant change has occurred.
- Keep a clear list of completed, pending, blocked, and failed acceptance criteria (P01-P43).

## 3. Test changes proportionately
- After a small change, run focused tests for the changed behavior.
- Run affected integration tests when a change crosses component boundaries.
- Run the broader test suite and CI at planned milestones, such as completion of a feature group or release candidate, rather than after every minor edit.
- Re-run previously passing tests only when a change could affect them, a dependency/environment changes, or a failure provides a concrete reason.

## 4. Work in coherent batches
- Inspect the remaining work once, make a consolidated implementation plan, and group related changes.
- Implement related changes together instead of repeatedly stopping after each file.
- Do not reopen completed work without new evidence of a defect or regression.
- If a defect is found, fix it and test the affected area; avoid restarting unrelated checks.

## 5. Report only meaningful progress
- Do not send repetitive progress reports or restate unchanged CI results.
- Report completed work with commit/test evidence, remaining work, blockers, and any owner action required.
- Never describe a check as passed unless its evidence exists. CI success does not substitute for required Android device acceptance.

## 6. Safety exceptions
A previously checked item MUST be checked again when there is a credible trigger, including:
- Relevant source or configuration changes.
- Dependency, build tool, compiler, model, or runtime changes that can affect the result.
- A failing test, crash, regression, security finding, or contradictory evidence.
- A changed acceptance requirement or target environment.
- Final acceptance/release verification explicitly required by the Phase 1 checklist.

Security-sensitive changes must receive appropriate targeted regression testing. This rule must never be used to skip necessary security validation.

## 7. Mandatory execution principle
**Check once, record the evidence, reuse it while it remains applicable, and spend the saved time completing unfinished work.**

Efficiency must not weaken correctness, security, data preservation, or the P01-P43 acceptance criteria. These rules do not authorize merging a pull request, changing signing credentials, uninstalling the app, clearing user data, or taking any owner-only action.
