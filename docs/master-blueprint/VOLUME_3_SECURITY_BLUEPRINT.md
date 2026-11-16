# MASTER BLUEPRINT — VOLUME 3: SECURITY BLUEPRINT

**Project:** Somchai Full System  
**Version:** 0.1  
**Status:** Initial Draft (Group C)  
**Date:** 2026-11-16  
**Source:** Master Development Instruction Parts 1, 2, 6 + Requirement Analysis

---

## 1. Purpose of this Volume

This volume defines the Security Architecture, Owner Authority model, Trust Boundaries, Risk classification, and all controls that protect the system from unauthorized actions — including actions originating from the AI itself.

Security Control Plane is strictly separated from the Capability Development Plane.

---

## 2. Trust Boundaries

| Boundary | Description | Trust Level | Notes |
|----------|-------------|-------------|-------|
| Owner | Human owner of the system | Highest | Sole authority for identity, policy, high-risk approval |
| Local Device (TECNO LK7) | Physical Android device | High (when authenticated) | Holds keys, Security Gate, Emergency Stop |
| Local Runtime | Processes running on device | Medium–High | Subject to Android lifecycle & permissions |
| Cloud Runtime | External compute / models | Low–Medium | Must never bypass Local Security Gate |
| AI Models / Providers | External or local models | Untrusted by default | Output is Untrusted Input |
| Plugins / Tools / External APIs | Third-party or generated tools | Untrusted | Require permission and validation |
| Candidate Code | Code produced by Self-Evolution | Untrusted until verified | Must stay in Isolated Sandbox |
| Uploaded Files / Web Content | External content | Untrusted | Subject to validation & taint tracking |
| Repository & CI/CD | Source and build pipeline | Medium | Supply chain controls required |

**Rule:** Anything crossing a trust boundary is subject to validation, authorization, and audit.

---

## 3. Threat Model (Initial)

### High-Priority Threats

| Threat ID | Threat | Attacker / Source | Impact | Mitigation Direction |
|-----------|--------|-------------------|--------|----------------------|
| T-01 | Self-Evolution modifies Security Policy to gain power | Internal (Candidate / SE) | Critical | Immutable Security Boundary + Plane separation |
| T-02 | Prompt Injection causes unauthorized tool use or data exfiltration | External content / Model | High–Critical | Untrusted Input handling + command/data separation |
| T-03 | Stolen or lost device → data exposure | Physical | High | Android Keystore, encryption, remote revoke (where possible) |
| T-04 | Candidate reaches Production without verification | Process failure | High | Independent Test Suite + Release Authority |
| T-05 | Cloud provider or model provider compromise | External | High | Local Security Gate, data classification, minimal data sent |
| T-06 | Supply chain attack via dependency or GitHub Action | External | High | Pinning, SBOM, provenance, least privilege tokens |
| T-07 | Privilege escalation via misconfigured permission | Internal misconfig | High | Permission Matrix + least privilege + audit |
| T-08 | Audit log tampering | Internal or compromised component | Critical | Audit Integrity controls, append-only design where possible |
| T-09 | Emergency Stop bypassed or disabled by AI | Internal | Critical | Emergency Stop independent of Brain / SE |
| T-10 | Resource exhaustion used to degrade security controls | Resource pressure | Medium–High | Resource Governor must not disable Security Plane |

---

## 4. Risk Matrix (R1–R4)

| Level | Name | Description | Typical Handling |
|-------|------|-------------|------------------|
| **R1** | Low | Routine, reversible, within granted permissions | May run autonomously |
| **R2** | Medium | Elevated impact or external interaction | Additional checks, logging, possible soft confirmation |
| **R3** | High | Significant data impact, irreversible effects, or security-relevant | Explicit Owner approval required (policy-based) |
| **R4** | Critical | Touches Security Boundary, Owner Authority, Identity, Emergency Stop, or Audit Integrity | Highest controls + Owner approval + strong evidence |

**Risk Gate** must be independent of Model Reasoning. The model cannot override or redefine the Risk Gate outcome.

---

## 5. Permission Matrix (Initial High-Level)

| Action Category | Default | R1 | R2 | R3 | R4 | Notes |
|-----------------|---------|----|----|----|----|-------|
| Read local non-sensitive data | Allow (within policy) | ✓ | ✓ | — | — | |
| Write / update Memory (non-critical) | Allow + Audit | ✓ | ✓ | — | — | |
| Call low-risk tool (read-only) | Allow + Audit | ✓ | Possible | — | — | |
| Network request (general) | Restricted | — | Check | Approve | — | |
| Execute code / shell | Deny by default | — | — | Approve | Approve | |
| Modify Security Policy | Deny | — | — | — | Owner only | |
| Change Owner Identity / Trusted Status | Deny | — | — | — | Owner only | |
| Disable Emergency Stop | Deny | — | — | — | Impossible by design | |
| Deploy Candidate to Production | Deny | — | — | Approve | Approve | Independent verification required |
| Access or export secrets | Deny | — | — | — | Owner only | |

This matrix will be expanded into fine-grained permissions per module and tool in later iterations.

---

## 6. Owner Authority, Identity & Session Lifecycle

### 6.1 Owner Authority Rules
- Owner is the single highest authority.
- AI cannot grant itself Trusted Status or expand its own Permission Boundary.
- High-impact changes (Security Policy, Identity, Trusted Status, Approval rules) require Owner action.
- Approvals are: action-specific, single-use, and time-bounded.

### 6.2 Identity & Session (Initial Requirements)
- Owner Identity enrollment and authentication must be device-bound where appropriate (Android Keystore / Biometric).
- Session creation, expiration, and revocation must be explicit.
- Successful authentication ≠ authorization for every action.
- Revocation must take effect according to defined scope and must not rely solely on the AI Brain.
- Lost device and device replacement procedures are mandatory (details in later recovery design).

### 6.3 Key Management (Initial)
- Use Android Keystore for device-bound keys where suitable.
- Android Keystore keys are generally non-exportable → plan for Device Loss Recovery.
- Separate Recovery Key from Data Encryption Key conceptually.
- Real cryptographic keys must not be generated or used with real secrets until Owner explicitly authorizes (Phase 0 uses Synthetic Data only).

---

## 7. Emergency Stop & Safe State

### Emergency Stop
- Must be independent of Brain and Self-Evolution.
- Must be able to halt agent activity and revoke relevant permissions.
- Design both local and (where feasible) remote revocation paths.
- Do not claim hardware kill-switch capability that does not exist on the device.

### Deterministic Safe State
When in Safe State or after Emergency Stop:
- No new high-risk actions.
- No Candidate deployment.
- Security Control Plane remains active.
- Audit continues.

---

## 8. Prompt Injection Defense

All of the following are **Untrusted Input**:
- Model output
- Web pages
- Uploaded files
- Plugin / Tool responses
- External APIs
- Generated code

**Controls (required direction):**
- Input validation and schema validation
- Clear separation between “data being read” and “commands that control the system”
- Allowlists where appropriate
- Provenance and taint tracking
- Never treat content found in documents or tool output as privileged instructions

---

## 9. Supply Chain Security (Initial)

- Maintain Dependency Inventory / SBOM direction
- Pin GitHub Actions to full commit SHA
- Least-privilege tokens
- No secrets in source code or unencrypted logs
- Artifact integrity verification
- Third-party code review proportionate to risk

---

## 10. Security Boundary for Self-Evolution

Self-Evolution **must not** be able to modify (directly or indirectly) to increase its own power:

- Owner Identity
- Owner Authority
- Trusted Status
- Permission Boundary
- Security Policy
- Risk Matrix
- Security Gate
- Approval Manager
- Emergency Stop
- Audit Integrity
- Independent Test Suite
- Release Signing Authority

Candidate code runs only in Isolated Development Sandbox until it passes Independent Verification and receives proper Release Authority.

---

## 11. Open Items / Pending Verification

| Item | Status | Notes |
|------|--------|-------|
| Detailed fine-grained Permission Matrix | Pending | To be expanded per module/tool |
| Concrete cryptographic key hierarchy | Pending | Awaits Owner authorization for real keys |
| Exact Emergency Stop UX and remote revoke feasibility | Pending | Device & platform constraints |
| Full Threat Model with attack trees | Pending | Will deepen in later security iterations |
| Numeric risk thresholds | Pending Verification | Must be measured or set by Owner |

---

## 12. Related Requirements (Traceability)

- REQ-SEC-001 to REQ-SEC-007
- REQ-SEV-004, REQ-SEV-005
- REQ-GOV-003, REQ-GOV-004
- RISK-001, RISK-002, RISK-005, RISK-008, RISK-009 (see Risk Register)

---

**Document Status:** Initial Security Blueprint (Group C) complete at high level.  
Ready for expansion and cross-volume consistency checks in later phases.
