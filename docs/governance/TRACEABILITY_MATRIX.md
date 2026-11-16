# Somchai Full System — Traceability Matrix (Initial Structure)

**Version:** 0.1  
**Status:** Skeleton  
**Purpose:** Bidirectional traceability between original requirements, Master Blueprint volumes, modules, tests, and implementation evidence.

---

## Matrix Structure

| Requirement ID | Source Document | Blueprint Volume | Section | Related Modules (A–M) | Interface / Schema | Acceptance Criteria | Test Cases | Implementation Evidence | Verification Status |
|----------------|-----------------|------------------|---------|-----------------------|--------------------|---------------------|------------|-------------------------|---------------------|
| REQ-SYS-001 | Part 1 | Vol 1 | System Scope | All | — | Full System delivered | — | — | Approved (Requirement) |
| REQ-SYS-002 | Part 1 | Vol 1 | Module Inventory | A–M | Interface Contracts | All 13 modules present with contracts | — | — | Approved (Requirement) |
| REQ-SEC-001 | Part 1, 2 | Vol 3 | Owner Authority | C, F, M | — | AI cannot self-elevate | — | — | Approved (Requirement) |
| REQ-SEC-002 | Part 1 | Vol 3 | Security Control Plane | F, M | — | Planes are separated | — | — | Approved (Requirement) |
| REQ-SEC-003 | Part 1, 2 | Vol 3 | Immutable Boundary | F, M | — | Boundary cannot be modified by SE | — | — | Approved (Requirement) |
| REQ-SEV-001 | Part 2 | Vol 1 | Self-Evolution | M | — | 15 sub-modules implemented | — | — | Approved (Requirement) |
| REQ-SEV-004 | Part 2 | Vol 3 | Candidate Isolation | M, F | Sandbox Contract | Candidates isolated until verified | — | — | Approved (Requirement) |
| REQ-RES-001 | Part 3 | Vol 7 | Resource Governor | A, All | Resource API | Governor operational | — | — | Approved (Requirement) |
| REQ-RES-003 | Part 3 | Vol 3, 7 | Resource vs Security | F, All | — | Security never reduced for resources | — | — | Approved (Requirement) |
| REQ-DEL-001 | Part 4 | Vol 1, 6 | Delivery | K | — | Operable from Android phone | — | — | Approved (Requirement) |
| REQ-GOV-003 | Part 6 | Vol 4 | Evidence-Based | All | — | No claim without evidence | — | — | Approved (Requirement) |

---

## Coverage Summary (Current)

- Requirements extracted: Initial set (see REQUIREMENT_REGISTRY.md)
- Mapped to Volumes: Partial (high-level only)
- Mapped to Modules: Partial
- Test Cases linked: 0 (to be added in Quality phases)
- Implementation Evidence: 0 (coding not yet authorized for core system)

---

## Next Actions for this Matrix
1. Expand granular requirements from all 6 parts
2. Complete mapping to 7 Blueprint Volumes
3. Link to specific A–M modules and Self-Evolution 15 sub-modules
4. Add Acceptance Criteria and Test Case IDs in later phases
5. Maintain bidirectional integrity (Source → Blueprint and Blueprint → Source)
