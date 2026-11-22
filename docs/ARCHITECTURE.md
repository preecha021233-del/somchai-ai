# SOMCHAI AI — Runtime Architecture

## Protected Core
Package com.somchai.ai.core contains CoreRuntime, PermissionManager, LlmConnector, SecretStore, PluginValidator, PluginManager, ActivityLogger and ResourceLimiter. Core code owns identity, policy, credentials, validation and activation.

## Mutable extensions
Extensions are JSON manifests stored in app-private staging, active and backup directories. A manifest selects only a finite operation already implemented by the core. It cannot supply executable code or modify core policy.

## Lifecycle
DRAFT -> STAGED -> VALIDATED -> AWAITING_APPROVAL -> ACTIVE. Owner approval is bound to the exact SHA-256 hash. Activation keeps a previous version and restores it if promotion or validation fails.

## Trust boundary
Model responses, network pages, files and extension manifests are untrusted. They are parsed as data. No shell, reflection-based loading, arbitrary JavaScript, downloaded APK or dynamic class loading is permitted.

## Storage
All state is inside app-private filesDir/somchai: core, plugins/staging, plugins/active, plugins/backups, plugin-data, memory and audit. Android sandboxing and application signing protect this data from other apps; in-process plugins are not a strong OS isolation boundary.
