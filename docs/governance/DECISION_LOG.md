# Decision Log — Current Architecture

| ID | Decision | Reason | Status |
|---|---|---|---|
| DEC-001 | preecha021233-del/somchai-ai is the authoritative repository | Confirmed by owner | Active |
| DEC-002 | Use Protected Core + declarative Sandboxed Plugins | Agent cannot change policy or execute arbitrary code | Active |
| DEC-003 | Owner approval is bound to exact plugin content hash | Prevents approval reuse after candidate mutation | Active |
| DEC-004 | Keep existing Compose design components and screens | Preserve useful work while wiring a functional app entry point | Active |
| DEC-005 | Android Keystore protects provider API keys | Avoid plaintext credentials in source or ordinary preferences | Active |

Previous 40-phase / 7-volume plans are superseded by the current architecture and release gates.
