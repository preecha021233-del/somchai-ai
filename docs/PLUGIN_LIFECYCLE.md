# Plugin Lifecycle Contract

States: DRAFT → STAGED → VALIDATED → AWAITING_APPROVAL → ACTIVE → SUSPENDED or ROLLED_BACK.

1. Create a manifest in staging.
2. Validate schema, identifier, version, content hash, permissions, operation allowlist, and resource limits.
3. Run deterministic smoke checks without granting network or secret access.
4. Show the owner the manifest, permissions, hash, and test result.
5. Receive one-time approval for that exact hash.
6. Snapshot the active manifest and promote the candidate atomically.
7. Run a health check; on failure restore the previous snapshot.
8. Record every transition in the audit log.

A manifest is data, not executable code. Unsupported operations are rejected. Candidate code cannot certify itself or modify the validator.
