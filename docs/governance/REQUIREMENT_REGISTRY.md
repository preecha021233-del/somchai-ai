# Somchai Full System — Requirement Registry

**Version:** 0.1  
**Status:** Initial Extraction  
**Source Documents:** Master Development Instruction Parts 1–6 + 40-Phase Plan  
**Date:** 2026-11-16

---

## Naming Convention

`REQ-[VOLUME/AREA]-[NUMBER]`

Examples:
- REQ-SEC-001 = Security related
- REQ-SEV-001 = Self-Evolution related
- REQ-RES-001 = Resource Governance related
- REQ-SYS-001 = System / Architecture related
- REQ-GOV-001 = Governance related

---

## High-Level Requirement Groups (Initial)

### 1. System & Architecture (REQ-SYS)

| ID | Statement (Summary) | Source | Priority | Status |
|----|---------------------|--------|----------|--------|
| REQ-SYS-001 | Develop as Full System; do not reduce to MVP | Part 1 | Critical | Approved |
| REQ-SYS-002 | Maintain all 13 modules A–M with clear responsibilities and Interface Contracts | Part 1 | Critical | Approved |
| REQ-SYS-003 | Mobile-first + Hybrid Local/Cloud architecture | Part 1, 4 | Critical | Approved |
| REQ-SYS-004 | Every module must have real Implementation, not only placeholder | Part 1 | Critical | Approved |
| REQ-SYS-005 | No module may access another module’s data without Contract and permission | Part 1 | High | Approved |

### 2. Owner Authority & Security (REQ-SEC)

| ID | Statement (Summary) | Source | Priority | Status |
|----|---------------------|--------|----------|--------|
| REQ-SEC-001 | Owner is the highest authority; AI cannot elevate itself | Part 1, 2 | Critical | Approved |
| REQ-SEC-002 | Security Control Plane must be separated from Capability Development Plane | Part 1 | Critical | Approved |
| REQ-SEC-003 | Immutable Security Boundary items cannot be modified by Self-Evolution to gain power | Part 1, 2 | Critical | Approved |
| REQ-SEC-004 | Risk Matrix R1–R4 must be implemented; Risk Gate independent of Model Reasoning | Part 1 | Critical | Approved |
| REQ-SEC-005 | Emergency Stop must not depend on Brain / Self-Evolution | Part 1 | Critical | Approved |
| REQ-SEC-006 | Approvals are single-use, action-specific, and time-bound | Part 1 | High | Approved |
| REQ-SEC-007 | Untrusted Input (Model output, web, files, tools) must be treated as untrusted | Part 1 | Critical | Approved |

### 3. Self-Evolution (REQ-SEV)

| ID | Statement (Summary) | Source | Priority | Status |
|----|---------------------|--------|----------|--------|
| REQ-SEV-001 | Implement full Self-Evolution Engine with 15 mandatory sub-modules | Part 2 | Critical | Approved |
| REQ-SEV-002 | Support evolution levels E1–E5 completely | Part 2 | Critical | Approved |
| REQ-SEV-003 | Follow 10-step cycle: Observe → Identify → Research → Verify → Design → Develop → Test → Evaluate → Preserve → Deploy | Part 2 | High | Approved |
| REQ-SEV-004 | Candidate code must run only in Isolated Sandbox until verified | Part 2 | Critical | Approved |
| REQ-SEV-005 | Candidate Generator cannot certify its own Candidate | Part 2 | Critical | Approved |
| REQ-SEV-006 | Low-risk evolution may run autonomously within policy; high-risk requires approval | Part 2 | High | Approved |

### 4. Resource Governance (REQ-RES)

| ID | Statement (Summary) | Source | Priority | Status |
|----|---------------------|--------|----------|--------|
| REQ-RES-001 | Implement Central Resource Governor | Part 3 | High | Approved |
| REQ-RES-002 | Support Lazy Loading, On-Demand Execution, Suspension & Unloading | Part 3 | High | Approved |
| REQ-RES-003 | Never reduce Security level to save resources | Part 3 | Critical | Approved |
| REQ-RES-004 | Design for TECNO LK7 (8 GB Physical RAM, Android 16) | Part 3, 4 | High | Approved |
| REQ-RES-005 | Physical RAM must not be counted as including Virtual RAM | Part 3, 4 | High | Approved |

### 5. Real-World & Delivery (REQ-DEL)

| ID | Statement (Summary) | Source | Priority | Status |
|----|---------------------|--------|----------|--------|
| REQ-DEL-001 | System must be buildable, installable, and operable primarily from Android phone | Part 4 | Critical | Approved |
| REQ-DEL-002 | Phase 0 uses only Synthetic Data; no real secrets | Part 4 | Critical | Approved |
| REQ-DEL-003 | Practical Feasibility Gate (12 questions) must be answered before technology choices | Part 4 | High | Approved |
| REQ-DEL-004 | APK must be installable and testable on real target device | Part 4 | Critical | Approved |

### 6. Governance & Quality (REQ-GOV)

| ID | Statement (Summary) | Source | Priority | Status |
|----|---------------------|--------|----------|--------|
| REQ-GOV-001 | Every requirement must have a stable Requirement ID and Traceability | Part 5, 6 | Critical | Approved |
| REQ-GOV-002 | Maintain Glossary, Decision Log, Risk Register, Traceability Matrix | Part 6 | High | Approved |
| REQ-GOV-003 | Evidence-Based Completion: no “Done/Passed/Secure” without evidence | Part 6 | Critical | Approved |
| REQ-GOV-004 | Fail Secure principle must be applied system-wide | Part 6 | Critical | Approved |
| REQ-GOV-005 | No Hidden Operations — autonomous actions must be auditable | Part 6 | High | Approved |

---

## Notes
- This is the **initial extraction**. More granular requirements will be added in later phases.
- All Critical items are treated as non-negotiable unless Owner explicitly changes them.
- Numeric thresholds (RAM limits, cost, RTO/RPO, etc.) remain **Pending Verification**.
