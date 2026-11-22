# Plugin Lifecycle

1. Candidate JSON is written to plugins/staging.
2. Core validator checks size, ID, version, permission allowlist and finite operation allowlist.
3. UI displays description, requested permissions and SHA-256.
4. Owner explicitly approves or rejects.
5. Approval is checked against the exact current candidate hash.
6. Existing active manifest is backed up before promotion.
7. Active manifest is parsed again; failures restore the previous copy.
8. Every lifecycle event is written to the activity log.

Only core-owned operations are executable. Plugin manifests are data, not code.
