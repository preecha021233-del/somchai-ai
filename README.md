# SOMCHAI AI

**Self-Evolving Modular AI Agent — Protected Core + Sandboxed Plugins**

SOMCHAI AI is an Android personal assistant being developed toward an independent, self-evolving system. Its long-term architecture is governed by `docs/IMPLEMENTATION_PLAN.md` (Master Command).

## Current implemented baseline

- Android app foundation using Kotlin, Jetpack Compose and Material 3.
- Chat UI and an OpenAI-compatible Chat Completions connector.
- API key encryption with Android Keystore (AES-GCM).
- Core permission allowlist and untrusted external-data boundary.
- Declarative plugin staging, validation, SHA-256-bound approval, activation and rollback.
- Built-in web search operation using DuckDuckGo Instant Answer API, invoked with `/search` after approval.
- Owner-inspectable memory notes and append-oriented activity log.
- Daily API request and local storage budgets.
- GitHub Actions build, unit tests and APK artifact upload.

**Important current limitation:** the chat response path in this repository still calls the configured external LLM connector. The independent internal reasoning, task-planning and learning engine described in the Master Command have not yet been implemented. The app can open without an API key, but LLM chat responses will not work until an owner-authorized provider is configured. Configuring an external provider is optional and must never be treated as a prerequisite for the project's long-term architecture. Do not create accounts, API keys, subscriptions or charges without the owner's explicit approval.

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
2. The app can be opened without configuring an external AI provider. The current chat response feature still depends on an OpenAI-compatible provider; only configure one if the owner explicitly chooses to do so. No provider, account, API key or paid service is mandatory to the project's architecture.
3. To use the currently implemented web-search operation, approve the staged Web Search plugin in Plugins and use `/search` followed by a query.
4. Local memory notes and the existing app UI can be inspected without configuring an LLM provider.

## Master Command and development status

The authoritative requirements, owner-approval rules, Protected Core/Evolution Zone architecture, phase gates, internal processing goals and self-evolution roadmap are in [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).

The current code is an early functional app baseline, not a completed independent AI brain and not a security-certified release. Phase 0 repository audit and subsequent implementation gates must be completed with evidence. Do not describe planned capabilities as implemented.

## Release limitations

DuckDuckGo Instant Answer is not exhaustive general web search. Device testing, owner signing-key setup, in-place upgrade verification, provider compatibility, encrypted backup, privacy disclosure, accessibility review and penetration testing remain release gates.
