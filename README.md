# SOMCHAI AI

**Self-Evolving Modular AI Agent — Protected Core + Sandboxed Plugins**

SOMCHAI AI is an Android personal assistant being developed toward an independent, self-evolving system. Its long-term architecture is governed by `docs/IMPLEMENTATION_PLAN.md` (Master Command).

## Current implemented baseline

- Android app foundation using Kotlin, Jetpack Compose and Material 3.
- Offline-first chat UI routed through a deterministic internal rule engine by default.
- Optional OpenAI-compatible Chat Completions connector, invoked only with the explicit `/ai` command.
- API key encryption with Android Keystore (AES-GCM).
- Core permission allowlist and untrusted external-data boundary.
- Declarative plugin staging, validation, SHA-256-bound approval, activation and rollback.
- Built-in web search operation using DuckDuckGo Instant Answer API, invoked with `/search` after approval.
- Structured local memory with add, search, retrieval, deletion, backup and restore; production data is encrypted with AES-256-GCM using Android Keystore.
- Deterministic task decomposition for research, development, troubleshooting and general tasks.
- Append-oriented activity log for internal processing and memory events.
- Daily API request and local storage budgets.
- GitHub Actions build, unit tests and APK artifact upload.

**Phase 1 implementation limitation:** the internal processor remains a deterministic, bounded rule engine. A native llama.cpp on-device inference runtime and an explicit installer for the official Qwen2.5-0.5B-Instruct GGUF model have been added on `phase-1/local-brain-runtime`; this integration is pending CI and real-device testing. It is not yet evidence of advanced or human-level reasoning, and unsupported requests must not be silently sent to an external provider. Optional provider requests require the explicit `/ai` command and an owner-configured provider. Do not create accounts, API keys, subscriptions or charges without the owner's explicit approval.

The current JSON plugin system selects a finite set of operations already implemented by the app. It is not an arbitrary code execution runtime and is not yet the full self-extension system described in the Master Command.

## App update compatibility — mandatory and unchanged

Published updates must install directly over the existing SOMCHAI AI installation. Keep the application ID `com.somchai.ai` and the exact same owner-controlled signing keystore/certificate for every release. Increase `versionCode` with every published update. Do not uninstall the old app or clear its data to work around a signing mismatch.

The release build uses these protected environment variables / GitHub Actions secrets:
- `SOMCHAI_SIGNING_KEYSTORE` (local keystore file path; in CI the workflow creates it from the base64 secret)
- `SOMCHAI_SIGNING_KEYSTORE_BASE64` (GitHub Actions only)
- `SOMCHAI_SIGNING_STORE_PASSWORD`
- `SOMCHAI_SIGNING_KEY_ALIAS`
- `SOMCHAI_SIGNING_KEY_PASSWORD`

Never commit the keystore or passwords. CI produces an owner-signed release APK only when all required secrets are configured. Debug APKs are development builds and are not the supported production update channel.

Before publishing an update, verify the application ID, increasing version code and signing-certificate SHA-256 against the previous owner-signed release, then test installing over the existing app while preserving user data. A signature or application-ID mismatch blocks release. Never replace the owner's signing key as an update workaround.

## Security boundary

The current agent does not execute arbitrary downloaded Kotlin, shell commands, APKs or JavaScript. Plugins are JSON manifests selecting a finite core-owned operation. The Protected Core is protected from agent modification by application packaging and release signing, not by a folder name alone. Internet results and model output are untrusted data. Android's app sandbox does not make in-process plugin code a strong OS isolation boundary.

## Build

GitHub Actions installs Gradle 8.9 and JDK 17, runs `testDebugUnitTest` and `assembleDebug`, then uploads the debug APK. If owner signing secrets are configured, it also builds and uploads an owner-signed release APK. Android SDK API 35 is used.

For local Android Studio use, open the repository as a Gradle project and use a locally installed Gradle 8.9 or generate a wrapper with: `gradle wrapper --gradle-version 8.9`.

## Setup

1. Install the owner-signed release APK for normal use and future in-place updates.
2. Internal supported commands work without an external AI provider. An optional provider is called only when the owner explicitly uses `/ai` and has configured the provider. No provider, account, API key or paid service is mandatory.
3. To use the currently implemented web-search operation, approve the staged Web Search plugin in Plugins and use `/search` followed by a query.
4. Use `/remember text`, `/recall query`, and `/forget id` for local memory. The Memory tab also supports adding, searching and deleting stored entries.

## Master Command and development status

The authoritative requirements, owner-approval rules, Protected Core/Evolution Zone architecture, phase gates, internal processing goals and self-evolution roadmap are in [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).

Phase 0 is closed based on repository audit and owner device update testing. Phase 1 implementation is in progress. The local model runtime is under CI/device verification and is not yet accepted as a completed independent AI brain or a security-certified release. Remaining Phase 1 acceptance gates must be completed with automated and device evidence. Do not describe planned capabilities as implemented.

## Release limitations

DuckDuckGo Instant Answer is not exhaustive general web search. Device testing, owner signing-key setup, in-place upgrade verification, provider compatibility, encrypted backup, privacy disclosure, accessibility review and penetration testing remain release gates.
