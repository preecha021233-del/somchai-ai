# Somchai Full System — Risk Register

**Version:** 0.1  
**Status:** Initial  
**Last Updated:** 2026-11-16

---

| Risk ID | Threat / Failure Scenario | Cause | Likelihood | Impact | Severity | Existing Controls | Mitigation | Detection | Recovery | Residual Risk | Owner Approval Required | Test Evidence |
|---------|---------------------------|-------|------------|--------|----------|-------------------|------------|-----------|----------|---------------|-------------------------|---------------|
| RISK-001 | AI modifies Security Policy to increase its own power | Self-Evolution or Candidate code | Medium | Critical | Critical | Immutable Security Boundary, Separation of Planes | Hard isolation of Security Control Plane; Independent Test Suite | Audit log + integrity checks | Rollback + Emergency Stop | Medium (depends on implementation quality) | Yes | Pending |
| RISK-002 | Candidate code reaches Production without verification | Weak release process | Medium | High | High | Candidate Isolation rule | Mandatory Independent Verification + Release Authority | Build/Release pipeline checks | Rollback to last verified version | Medium | Yes | Pending |
| RISK-003 | Device loss leads to permanent data loss | No tested recovery | Medium | High | High | Backup requirement (stated) | Encrypted off-device backup + tested restore | Backup verification tests | Device Loss Recovery procedure | High until tested | Yes | Pending |
| RISK-004 | Resource exhaustion on TECNO LK7 (8GB RAM) | Uncontrolled module loading | High | Medium | High | Resource Governance requirements | Lazy Loading, Resource Governor, Priority Allocation | Resource metrics + alerts | Graceful Degradation + unload non-critical modules | Medium | No (operational) | Pending |
| RISK-005 | Prompt Injection causes unauthorized action | Untrusted input treated as command | High | High | Critical | Untrusted Input classification | Input Validation, Taint Tracking, Separation of data vs command | Security tests | Deny action + Audit | Medium | Yes | Pending |
| RISK-006 | Cloud provider outage blocks critical functions | Over-reliance on single provider | Medium | Medium | Medium | Hybrid Local/Cloud design | Local critical path + provider failover | Provider health checks | Local queue + resume | Medium | No | Pending |
| RISK-007 | Real secrets or personal data used in early phases | Process violation | Medium | High | High | Phase 0 Synthetic Data rule | Strict Phase 0 policy + code review | Secret scanning | Revoke + rotate | Low if followed | Yes | Pending |
| RISK-008 | Background execution killed by Android 16 | Platform restrictions | High | Medium | Medium | WorkManager + Foreground Service guidance | Checkpoint + durable queue | Process death tests | Resume from checkpoint | Medium | No | Pending |
| RISK-009 | Cost overrun from uncontrolled API/token usage | No budget controls | Medium | Medium | Medium | Cost Governance requirements | Budget Guard + Circuit Breaker | Usage monitoring | Suspend paid tasks | Medium | Yes (budget) | Pending |
| RISK-010 | Incomplete Traceability leads to missed requirements | Documentation gaps | Medium | High | High | Traceability Matrix requirement | Bidirectional matrix + review | Coverage checks | Gap analysis + remediation | Medium until complete | No | Pending |

---

### Severity Scale
- Critical: Threatens Owner Authority, Security Boundary, or data integrity
- High: Significant functional or safety impact
- Medium: Degraded capability or increased operational burden
- Low: Minor inconvenience

### Notes
- All numeric thresholds (RAM, cost, RTO/RPO) remain **Pending Verification** until measured on real device or set by Owner.
- This register will be expanded as Threat Modeling (Phase 11+) progresses.
