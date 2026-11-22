# SOMCHAI AI

**Self-Evolving Modular AI Agent — Protected Core + Sandboxed Plugins**

SOMCHAI AI is an Android personal assistant built around an immutable-by-agent core and owner-approved declarative extensions.

## Implemented baseline
- Android app foundation using Kotlin, Jetpack Compose and Material 3.
- Chat UI and OpenAI-compatible Chat Completions connector.
- API key encryption with Android Keystore (AES-GCM).
- Core permission allowlist and untrusted external-data boundary.
- Declarative plugin staging, validation, SHA-256-bound approval, activation and rollback.
- Built-in web search operation using DuckDuckGo Instant Answer API, invoked with /search query after approval.
- Owner-inspectable memory notes and append-oriented activity log.
- Daily API request and local storage budgets.
- GitHub Actions build, unit tests and APK artifact upload.

## Security boundary
The agent does not execute arbitrary downloaded Kotlin, shell commands, APKs or JavaScript. Plugins are JSON manifests selecting a finite core-owned operation. The Protected Core is protected from agent modification by application packaging and release signing, not by a folder name alone. Internet results and model output are untrusted data.

## Build
GitHub Actions installs Gradle 8.9 and JDK 17, runs testDebugUnitTest and assembleDebug, then uploads the debug APK. Android SDK API 35 is used.

For local Android Studio use, open the repository as a Gradle project and use a locally installed Gradle 8.9 or generate a wrapper with: gradle wrapper --gradle-version 8.9.

## Setup
1. Install the generated debug APK.
2. Open Settings and enter an HTTPS OpenAI-compatible API base URL, model and API key.
3. Approve the staged Web Search plugin in Plugins.
4. Chat with SOMCHAI. Use /search followed by a query for web lookup.

## Release limitations
This is a functional initial app baseline, not a security-certified release. DuckDuckGo Instant Answer is not exhaustive general web search. Device testing, provider compatibility, encrypted backup, release signing, privacy disclosure, accessibility review and penetration testing remain release gates.
