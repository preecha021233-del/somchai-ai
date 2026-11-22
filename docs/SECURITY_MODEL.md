# Security Model

- Default deny. Permissions are core-owned and extension declarations cannot expand the allowlist.
- API credentials are encrypted with AES-GCM using a non-exportable Android Keystore key.
- Provider endpoints must use HTTPS.
- Network and model output are untrusted data, never executable instructions.
- Plugin manifest size, identifier, version, permissions and operation are validated.
- Approval is bound to the exact candidate SHA-256; changed content requires a new review.
- Activation snapshots the previous active manifest and restores it if validation or promotion fails.
- API requests are limited to 40/day; SOMCHAI app-private storage is budgeted to 50 MB.
- No arbitrary code execution is supported.

## Known gaps before production release
- No encrypted off-device backup or tested restore workflow yet.
- Audit file is append-oriented, not cryptographically tamper-evident.
- Runtime rollback covers plugin manifest activation, not APK/app update rollback.
- Provider TLS relies on Android platform defaults; certificate pinning is not configured.
- Device, API compatibility, penetration and accessibility tests remain required.
