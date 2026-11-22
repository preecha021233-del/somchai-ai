# Risk Register

| Risk | Mitigation in current code | Remaining work |
|---|---|---|
| Prompt injection | External data treated as data; fixed operation allowlist | Adversarial prompt-injection test suite |
| Plugin privilege escalation | Core permission allowlist; manifest validation | Formal security review |
| Candidate mutation after approval | SHA-256 checked at approval time | Persisted signed approval records |
| Broken plugin activation | Backup and restore on activation failure | Fault-injection tests |
| Secret exposure | AES-GCM key in Android Keystore; HTTPS only | Backup/restore and provider-specific audit |
| Resource exhaustion | 40 API requests/day; 50 MB local storage budget | Battery/network quotas and log rotation |
| Data loss | App-private local storage | Encrypted export and verified restore |
| Build regression | GitHub Actions build/test workflow | First successful CI run and device verification |

Remaining risks are release gates, not claims of completed security certification.
