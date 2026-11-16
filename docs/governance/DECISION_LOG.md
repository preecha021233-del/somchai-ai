# Somchai Full System — Decision Log

**Version:** 0.1  
**Status:** Active  
**Purpose:** Record architecture and requirement decisions with rationale and impact.

---

| Decision ID | Date | Proposed By | Decided By | Problem / Question | Options Considered | Decision | Rationale | Impact | Status | Affected Documents |
|-------------|------|-------------|-----------|--------------------|--------------------|----------|-----------|--------|--------|--------------------|
| DEC-001 | 2026-11-16 | Grok | Owner (pending) | Which repository is authoritative? | preechaphukthaisong65-arch vs preecha021233-del | Use `preecha021233-del/somchai-ai` | Confirmed by Owner on 2026-11-16 | All future work targets this repo | Confirmed | All |
| DEC-002 | 2026-11-16 | Master Instruction | Owner | Development approach | MVP vs Full System | Full System only | Explicit prohibition of scope reduction | Entire project scope | Confirmed | Parts 1–6 |
| DEC-003 | 2026-11-16 | Master Instruction | Owner | When can coding begin? | Immediate vs after Blueprint | Coding only after explicit Owner order | Prevent premature implementation | Process control | Confirmed | Part 5, Phase Plan |
| DEC-004 | 2026-11-16 | Grok | Owner (pending) | Branch strategy | main only vs full-system-development | Pending Owner confirmation | Documents specify `full-system-development` | Branch protection & workflow | Open | Part 1, Part 5 |

---

### Rules for this Log
- Every significant architecture or requirement decision must be recorded.
- Do not delete previous decisions; mark as Superseded if replaced.
- Security and Owner Authority decisions require explicit Owner approval.
