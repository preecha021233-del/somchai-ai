# SOMCHAI Learned Skills

## What a skill is

A learned skill is an encrypted, reusable procedure prompt. It is data, not executable Kotlin, JavaScript, shell, or downloaded code. It cannot grant permissions or create new plugin operations.

## Owner-controlled workflow

1. Use `/skill learn` to ask the installed local model to extract reusable steps from a successful experience, or `/skill propose` to provide the procedure directly.
2. Supply 2–10 test cases in the form `input => expected text`.
3. Run `/skill test <id>`. The local model executes each example using the proposed procedure.
4. The candidate remains inactive until every test passes.
5. The owner explicitly runs `/skill approve <id>`.
6. Approved procedures are included in the local Brain prompt only when one of their trigger phrases matches the user's request.

## Commands

```text
/skill list
/skill learn ชื่อ | คำกระตุ้น,คำกระตุ้น | ประสบการณ์ที่สำเร็จ | input1 => expected1 ;; input2 => expected2
/skill propose ชื่อ | คำกระตุ้น,คำกระตุ้น | ขั้นตอน | input1 => expected1 ;; input2 => expected2
/skill test รหัส
/skill approve รหัส
/skill delete รหัส
```

## Storage and limits

- Skills are stored in app-private `files/somchai/skills/skills.enc`.
- Production storage uses AES-256-GCM with Android Keystore through the existing memory cipher.
- Maximum 100 skills.
- Each skill has 1–10 trigger phrases, 2–10 test cases, and a bounded procedure.
- Only approved skills are retrieved for ordinary chat.
- Deleting a skill removes it from future prompt retrieval.

## Honest limitations

- The model drafts a procedure from an owner-provided successful experience; it does not autonomously rewrite application code or invent new native tools.
- Evaluation currently checks whether the generated answer contains the expected text for each test. This is a basic smoke test, not a strong semantic benchmark or proof of general reliability.
- The same local model drafts and executes the skill tests, so independent review and broader regression tests are still needed.
- Skills do not execute arbitrary actions. Tool execution still goes through the existing permission-gated core operations.
- Do not describe this as autonomous self-improvement or self-modifying code.
