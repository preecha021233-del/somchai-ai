# Security Model

## Protected invariants

- Owner authority cannot be granted by model output or a plugin.
- Permission checks are performed by core code, outside extension-controlled data.
- Internet pages, model responses, files, and tool outputs are tainted untrusted input.
- A plugin cannot access API credentials or alter its own permission declaration after approval.
- Approval is bound to plugin ID, exact content hash, requested permissions, and a short expiry.
- Core policy and validator are not runtime-editable.
- Resource exhaustion must not disable security checks.

## Threats and controls

| Threat | Required control |
|---|---|
| Prompt injection | Treat external text as data; structured proposals; policy gate |
| Plugin privilege escalation | Permission allowlist; immutable approved manifest hash |
| Path traversal | Canonical path containment; reject absolute paths and parent segments |
| Malicious package | Schema, size, operation, and permission validation |
| Broken update | Backup before promotion; health check; rollback |
| Secret leakage | No secrets in source, logs, or plugin context |
| Quota exhaustion | Request, token, and storage budgets with fail-closed behavior |

## Approval

Approval is explicit, one-time, candidate-specific, and expires. An approval for one plugin version does not authorize a later version.

## Limitations

A Kotlin package in the same Android process is not a strong security sandbox. The initial implementation therefore limits extensions to declarative operations and does not promise isolation for arbitrary code. Production-grade arbitrary-code execution would require a separately reviewed OS process/container boundary and a narrower IPC contract.
