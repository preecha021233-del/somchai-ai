# Somchai Full System — Glossary

**Version:** 0.1  
**Status:** Initial  
**Source:** Master Development Instruction Parts 1–6  
**Last Updated:** 2026-11-16

---

## A

**A–M Modules**  
The 13 core architectural modules of Somchai:  
A Core & Runtime, B Brain & Intelligence, C Identity & Owner, D Memory, E Knowledge, F Security, G ToolRunner & Integration, H Automation, I Audit & Monitoring, J Backup & Recovery, K Android Application, L Advanced Capabilities, M Self-Evolution Architecture.

**Acceptance Criteria**  
Pre-defined conditions that must be met before a requirement or module is considered done. Must be defined before testing begins.

**Audit Integrity**  
The requirement that audit logs cannot be altered, deleted, or hidden by the system or by Self-Evolution candidates.

---

## C

**Candidate**  
A proposed change (code, skill, tool, architecture, model) that has not yet passed Independent Verification and Release Authority.

**Capability Development Plane**  
The part of the system responsible for creating and improving capabilities. It is strictly separated from the Security Control Plane.

**Central Resource Governor**  
The system component that monitors and allocates device and cloud resources across all modules.

---

## E

**E1–E5**  
The five levels of Self-Evolution capability:  
- E1 Knowledge Evolution  
- E2 Capability Evolution  
- E3 Code Evolution  
- E4 Architecture Evolution  
- E5 Continuous Evolution Management

**Emergency Stop**  
A mechanism that can halt agent activity and revoke permissions independently of the Brain / Self-Evolution system.

**Evidence-Based Completion**  
The rule that no status (Implemented, Tested, Secure, Released) may be claimed without verifiable evidence.

---

## F

**Fail Secure**  
When an error or uncertainty occurs, the system must not grant additional privileges, skip authentication/authorization, or allow unapproved candidates into production.

**Full System**  
The complete Somchai system as defined in the Master Blueprint. Reducing scope to MVP is explicitly forbidden.

---

## I

**Immutable Security Boundary**  
The set of elements that Self-Evolution and Candidate code are forbidden from modifying to increase their own power (Owner Identity, Permission Boundary, Security Policy, Risk Matrix, Emergency Stop, Audit Integrity, Independent Test Suite, Release Signing Authority).

**Independent Test Suite**  
A test suite that Candidate code cannot modify or influence in order to make itself pass.

---

## O

**Owner**  
The single highest authority. Defines goals, policies, permissions, and high-impact approvals. AI cannot elevate itself to Owner or Trusted status.

**Owner Authority**  
The absolute control of the Owner over identity, permissions, security policy, and high-risk actions.

---

## R

**R1–R4 Risk Levels**  
- R1: Low risk — can run within granted permissions  
- R2: Medium risk — additional constraints and checks required  
- R3: High risk — requires policy-based approval  
- R4: Critical / Security Boundary impact — highest controls required

**Requirement ID**  
A unique, stable identifier assigned to every requirement for traceability.

**Residual Risk**  
Risk that remains after mitigation measures have been applied. Must be explicitly recorded.

---

## S

**Security Control Plane**  
The isolated layer that controls Identity, Permission, Risk Gate, Approval, Trusted Status, Emergency Stop, and Audit Integrity. Capability Development Plane cannot modify it to gain power.

**Self-Evolution**  
The system capability to learn, diagnose, improve knowledge, skills, code, architecture, and models under strict governance and isolation rules.

**Synthetic Data**  
Artificially generated data used in Phase 0. Real personal data and real secrets are forbidden until explicitly authorized.

---

## T

**Trusted Status**  
A status that can only be granted or revoked by the Owner. AI cannot grant Trusted Status to itself.

**Traceability Matrix**  
A bidirectional mapping between original requirements, Master Blueprint volumes, modules, tests, and implementation evidence.
