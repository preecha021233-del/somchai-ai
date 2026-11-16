# Somchai Full System — Requirement Analysis (Group B)

**Version:** 0.1  
**Date:** 2026-11-16  
**Phases Covered:** 7–10 of the 40-phase plan

---

## 1. วิเคราะห์ข้อกำหนดที่ซ้ำกัน (Duplicate Analysis)

ข้อกำหนดที่ปรากฏซ้ำในหลายส่วน แต่ความหมายสอดคล้องกัน สามารถรวมเป็นข้อกำหนดกลางได้

| หัวข้อที่ซ้ำ | ปรากฏใน | สรุปการรวม | Requirement ID หลัก |
|-------------|---------|------------|---------------------|
| ห้ามลดขอบเขตเป็น MVP | Part 1, 2, 5, 6, Phase Plan | รวมเป็นข้อบังคับสูงสุด | REQ-SYS-001 |
| Owner เป็นอำนาจสูงสุด + AI ห้ามยกระดับตัวเอง | Part 1, 2, 6 | รวมชัดเจน | REQ-SEC-001 |
| แยก Security Control Plane ออกจาก Capability Development | Part 1, 2 | รวม | REQ-SEC-002 |
| Immutable Security Boundary | Part 1, 2, 6 | รวมและขยายรายการ | REQ-SEC-003 |
| Candidate ต้องถูก Isolate + Independent Verification | Part 2, 6 | รวม | REQ-SEV-004, REQ-SEV-005 |
| ห้ามลด Security เพื่อประหยัดทรัพยากร | Part 3, 6 | รวม | REQ-RES-003 |
| Evidence-Based Completion | Part 1, 6 | รวม | REQ-GOV-003 |
| Fail Secure | Part 6 + หลักบังคับ | รวม | REQ-GOV-004 |
| ต้องมี Traceability | Part 5, 6 | รวม | REQ-GOV-001 |
| Hybrid Local/Cloud + Local ควบคุม Security | Part 1, 3, 4 | รวม | REQ-SYS-003 |
| ออกแบบสำหรับอุปกรณ์จริง (TECNO LK7 / Android 16) | Part 3, 4 | รวม | REQ-RES-004 |
| Phase 0 ใช้ Synthetic Data เท่านั้น | Part 4 | คงไว้ | REQ-DEL-002 |

**สรุป:** ไม่พบการซ้ำที่ขัดแย้งกันเองในกลุ่มนี้ สามารถรวมเป็นข้อกำหนดกลางได้โดยไม่สูญเสียเจตนารมณ์

---

## 2. วิเคราะห์ข้อที่อาจขัดแย้งกัน (Conflict Analysis)

| ประเด็น | ฝั่ง A | ฝั่ง B | การวิเคราะห์ | สถานะ | แนวทาง |
|--------|--------|--------|--------------|--------|--------|
| ความอิสระของ Self-Evolution vs การควบคุมของ Owner | Part 2 อนุญาตให้งานเสี่ยงต่ำทำเองได้ | Part 1 & 6 ย้ำ Owner Authority สูงสุด | ไม่ขัดแย้งโดยตรง — งานเสี่ยงต่ำทำได้ แต่งานกระทบ Security/Authority ต้องขออนุมัติ | Resolved | ใช้ Risk Matrix R1–R4 เป็นตัวแบ่ง |
| การใช้ Cloud vs ความปลอดภัย | อนุญาตใช้ Cloud สำหรับงานหนัก | ห้าม Cloud ข้าม Local Security Gate | ไม่ขัดแย้ง — Cloud อยู่ภายใต้ Local Gate เสมอ | Resolved | คงตามเดิม |
| เป้าหมาย 10,000 LOC/วัน vs Quality Gate | Part 1 ตั้งเป้าหมาย LOC | Part 1 และ 6 ย้ำ Quality เหนือจำนวนบรรทัด | ไม่ขัดแย้ง — LOC เป็นเป้าหมาย ไม่ใช่ข้อบังคับคุณภาพ | Resolved | Quality Gate มีอำนาจเหนือกว่า |
| Repository ชื่อ | บางส่วนระบุ preechaphukthaisong65-arch | Owner ยืนยัน preecha021233-del | ขัดแย้งด้านชื่อ | Resolved by Owner | ใช้ preecha021233-del/somchai-ai |
| Branch หลัก | เอกสารระบุ full-system-development | ปัจจุบันใช้ main | ยังไม่ชัด | Open | รอ Owner ยืนยัน (DEC-004) |
| ตัวเลข Threshold (RAM, Cost, RTO ฯลฯ) | หลายส่วนห้ามตั้งตัวเลขเอง | บางส่วนพูดถึงการติดตาม | ไม่ขัดแย้ง — ต้องวัดจริงหรือให้ Owner กำหนด | Resolved | ทุกตัวเลข = Pending Verification |

**สรุปข้อขัดแย้งที่ยังเปิดอยู่:**  
- Branch strategy (main vs full-system-development) → รอ Owner

---

## 3. ผลโหวตและข้อเสนอจาก AI

จากเอกสารที่ได้รับ **ยังไม่พบรายละเอียดผลโหวตหรือข้อเสนอจาก AI ตัวอื่น** ที่ถูกบันทึกมาอย่างเป็นระบบ

**สถานะ:**  
- Pending — ยังไม่มีข้อมูลผลโหวตให้วิเคราะห์ในรอบนี้  
- หากมีเอกสารผลโหวตเพิ่มเติม ควรนำมาใส่ใน Decision Log และวิเคราะห์ในรอบถัดไป

---

## 4. Mapping ข้อกำหนดเข้า 7 เล่ม (Master Blueprint Volumes)

### 18 กลุ่มจากส่วนที่ 5 + 15 กลุ่มจากส่วนที่ 6

| กลุ่มข้อกำหนด | เล่มหลักที่วาง | หมายเหตุ |
|---------------|----------------|----------|
| Module Inventory | เล่ม 1 | |
| Dependency Map | เล่ม 1 | |
| File Manifest | เล่ม 1 | |
| Interface Contracts | เล่ม 1 | |
| Database Design | เล่ม 2 | |
| API Contracts | เล่ม 2 | |
| Permission Matrix | เล่ม 3 | |
| Threat Model | เล่ม 3 | |
| Acceptance Criteria | เล่ม 4 | |
| Test Inventory | เล่ม 4 | |
| Provider Assessment | เล่ม 7 | |
| Resource Baseline | เล่ม 7 | |
| Delivery Architecture | เล่ม 1 + 5 | |
| User Workflow | เล่ม 6 + 4 | |
| Observability | เล่ม 5 + 7 | |
| Legal & Compliance | เล่ม 6 | |
| Operational Runbooks | เล่ม 5 | |
| Release & Rollout Strategy | เล่ม 5 + 1 | |
| Requirements Governance | เล่ม 1 | จากส่วนที่ 6 |
| Architecture Decision Records | เล่ม 1 | จากส่วนที่ 6 |
| Non-Functional Requirements | เล่ม 4 | จากส่วนที่ 6 |
| System Trust Boundaries | เล่ม 3 | จากส่วนที่ 6 |
| Data Flow & Privacy Mapping | เล่ม 2 | จากส่วนที่ 6 |
| Identity & Session Lifecycle | เล่ม 3 | จากส่วนที่ 6 |
| AI Behavior Governance | เล่ม 1 + 4 | จากส่วนที่ 6 |
| Model & Prompt Governance | เล่ม 1 + 7 | จากส่วนที่ 6 |
| API & Integration Lifecycle | เล่ม 1 + 2 | จากส่วนที่ 6 |
| Deployment & Installation Lifecycle | เล่ม 1 + 5 | จากส่วนที่ 6 |
| Software Supply Chain | เล่ม 3 + 5 | จากส่วนที่ 6 |
| Disaster Recovery & Continuity | เล่ม 2 + 5 | จากส่วนที่ 6 |
| Cost Forecasting & Financial Control | เล่ม 5 + 7 | จากส่วนที่ 6 |
| Human Factors & Accessibility | เล่ม 6 | จากส่วนที่ 6 |
| Legal, Licensing & Data Rights | เล่ม 6 | จากส่วนที่ 6 |

**หลักบังคับ 5 ข้อ (No Single Point of Failure, Fail Secure, Evidence-Based Completion, Backward Compatibility, No Hidden Operations)** → เป็นข้อกำหนดกลางของทุกเล่ม

---

## 5. สรุปผลการวิเคราะห์กลุ่ม B

- ข้อกำหนดซ้ำส่วนใหญ่รวมได้โดยไม่สูญเสียความหมาย
- ข้อขัดแย้งที่แท้จริงมีน้อย ส่วนใหญ่ Resolve ได้ด้วย Risk Matrix และการแยก Plane
- ยังขาดข้อมูลผลโหวตจาก AI อื่น
- Mapping เข้า 7 เล่มทำได้ในระดับกลุ่มแล้ว พร้อมขยายรายละเอียดในระยะถัดไป
- ประเด็นค้างหลัก: Branch strategy (DEC-004)

**เอกสารนี้พร้อมใช้เป็นฐานสำหรับกลุ่ม C (เล่ม 3 Security) ต่อไป**
