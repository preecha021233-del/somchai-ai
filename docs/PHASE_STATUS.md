# SOMCHAI AI — Phase Status

Updated: 2026-11-22

## Phase 0 — Repository Audit

**Status: CLOSED — original audit. Supplemental repository audit completed 2026-11-22; see docs/PHASE0_SUPPLEMENTAL_AUDIT.md.**

Completed:
- Repository structure and key application modules audited.
- Chat, plugin, memory, research/search, settings, security, build and signing configuration reviewed.
- GitHub Actions build completed successfully for debug and release APKs.
- Owner-signed release APK installed over the existing app on the owner's Android device.
- App launch verified after update.
- Existing settings verified as preserved after update.
- In-place installation succeeded; Android accepted the update over the installed application.

Observed limitation:
- Chat test returned HTTP 429 / `insufficient_quota` / `credit_balance_exhausted` from the configured external LLM provider. This is an external provider quota/billing limitation, not evidence of an APK installation failure.
- The chat test does not pass. Phase 1 must make core processing work without requiring an external AI provider.
- Chat history preservation was not separately confirmed.
- No claim is made that every application feature passed device testing.

## Phase 1 — Baby SOMCHAI

**Status: IMPLEMENTATION IN PROGRESS — native llama.cpp/Qwen runtime, direct model-test route and JNI UTF-8 fixes added; latest ARM64 CI run is pending; device/model acceptance remains open. Current candidate: versionCode 12 / versionName 1.9.2.**

Owner authorization to complete the remaining Phase 1 implementation was reaffirmed on 2026-11-22. The approved scope remains the five capabilities below, plus the approved Chat UI alignment to the existing Figma reference. No Phase 2 work is included. See `docs/PHASE1_PROGRESS.md` for implementation details and final acceptance gates.

Scope is limited to the five capabilities defined in `docs/IMPLEMENTATION_PLAN.md` Part 11:
1. Internal Processing
2. Research
3. Local Memory
4. Basic Plugin Development
5. Protection and Recovery

Implementation priorities:
1. Establish offline-capable internal processing and deterministic rule-based responses without an external LLM.
2. Add task decomposition, planning, tool selection and result verification.
3. Implement structured local memory with retrieval and deletion.
4. Connect an actual on-device language model to the local-first Brain interface; llama.cpp JNI runtime and verified Qwen model installer have been added on the development branch; ARM64 CI passed, while target-device benchmarks remain pending.
5. Improve research with actual linked-page reading while keeping internet access permission-gated and treating retrieved content as untrusted.
6. Make the declarative plugin lifecycle functional, permission-gated and owner-approved.
7. Improve protection, audit logging, backup, recovery and rollback.
8. Add automated tests and verify the existing application update/signing rules remain intact.

## Release and signing invariants

- Keep application ID `com.somchai.ai`.
- Keep the existing owner-controlled signing key and certificate.
- Increase `versionCode` for each release.
- Preserve user data and settings during normal updates.
- Never solve a signature mismatch by asking the owner to uninstall the existing app or clear its data.
- Do not expose signing credentials or API secrets.

This status record does not change the Master Command or authorize external services, paid usage, new accounts, or changes to signing credentials.
