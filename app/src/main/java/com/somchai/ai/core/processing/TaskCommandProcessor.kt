package com.somchai.ai.core.processing

/** User-facing command adapter for the durable task store. Step numbers in chat are 1-based. */
class TaskCommandProcessor(private val store: TaskExecutionStore) {
    fun execute(command: String): String {
        val clean = command.trim()
        require(clean.startsWith("/task", ignoreCase = true)) { "คำสั่งงานไม่ถูกต้อง" }
        val parts = clean.split(Regex("\\s+"), limit = 3)
        val action = parts.getOrNull(1)?.lowercase().orEmpty()
        return when (action) {
            "list" -> listTasks()
            "show" -> {
                require(parts.size >= 3) { "รูปแบบ: /task show รหัสงาน" }
                showTask(requireTask(parts[2]).id)
            }
            "start", "complete", "retry", "cancel" -> {
                require(parts.size >= 3) { "รูปแบบ: /task $action รหัสงาน ลำดับขั้นตอน" }
                val args = parts[2].split(Regex("\\s+"))
                require(args.size == 2) { "ต้องระบุรหัสงานและลำดับขั้นตอน เช่น /task $action abc123 1" }
                val index = parseStep(args[1])
                val task = when (action) {
                    "start" -> store.startStep(args[0], index)
                    "complete" -> store.completeStep(args[0], index)
                    "retry" -> store.retryStep(args[0], index)
                    else -> store.cancelStep(args[0], index)
                }
                "อัปเดตงาน ${task.id.take(8)} แล้ว\n" + render(task)
            }
            "fail" -> {
                require(parts.size >= 3) { "รูปแบบ: /task fail รหัสงาน ลำดับขั้นตอน | สาเหตุ" }
                val split = parts[2].split("|", limit = 2)
                require(split.size == 2) { "ต้องระบุสาเหตุหลังเครื่องหมาย |" }
                val args = split[0].trim().split(Regex("\\s+"))
                require(args.size == 2) { "ต้องระบุรหัสงานและลำดับขั้นตอน" }
                val task = store.failStep(args[0], parseStep(args[1]), split[1].trim())
                "บันทึกขั้นตอนว่าล้มเหลวแล้ว\n" + render(task)
            }
            else -> help()
        }
    }

    private fun listTasks(): String {
        val tasks = store.all()
        if (tasks.isEmpty()) return "ยังไม่มีงานที่บันทึกไว้"
        return "รายการงาน (${tasks.size})\n" + tasks.take(30).joinToString("\n\n") { task ->
            val completed = task.steps.count { it.status == TaskStepStatus.COMPLETED }
            val failed = task.steps.count { it.status == TaskStepStatus.FAILED }
            "• ${task.id.take(8)} — ${task.goal}\n  $completed/${task.steps.size} ขั้นตอนเสร็จ" +
                if (failed > 0) " • ล้มเหลว $failed ขั้น" else ""
        }
    }

    private fun showTask(id: String): String {
        val task = requireTask(id)
        return "งาน ${task.id}\nเป้าหมาย: ${task.goal}\n" + render(task)
    }

    private fun render(task: TaskRecord): String = task.steps.mapIndexed { index, step ->
        val state = when (step.status) {
            TaskStepStatus.PENDING -> "รอดำเนินการ"
            TaskStepStatus.IN_PROGRESS -> "กำลังทำ"
            TaskStepStatus.COMPLETED -> "เสร็จแล้ว"
            TaskStepStatus.FAILED -> "ล้มเหลว: " + (step.failureReason ?: "ไม่ระบุสาเหตุ")
            TaskStepStatus.CANCELLED -> "ยกเลิกแล้ว"
        }
        "${index + 1}. ${step.title} — $state"
    }.joinToString("\n")

    private fun requireTask(id: String): TaskRecord =
        store.get(id.trim()) ?: error("ไม่พบงานรหัส $id")

    private fun parseStep(value: String): Int {
        val number = value.toIntOrNull() ?: error("ลำดับขั้นตอนต้องเป็นตัวเลข")
        require(number > 0) { "ลำดับขั้นตอนเริ่มจาก 1" }
        return number - 1
    }

    private fun help(): String =
        "คำสั่งจัดการงาน:\n" +
            "/task list — ดูงานทั้งหมด\n" +
            "/task show รหัส — ดูรายละเอียดงาน\n" +
            "/task start รหัส ลำดับ — เริ่มขั้นตอน\n" +
            "/task complete รหัส ลำดับ — ทำเครื่องหมายว่าเสร็จ\n" +
            "/task fail รหัส ลำดับ | สาเหตุ — บันทึกความล้มเหลว\n" +
            "/task retry รหัส ลำดับ — ส่งขั้นตอนที่ล้มเหลวกลับไปรอทำใหม่\n" +
            "/task cancel รหัส ลำดับ — ยกเลิกขั้นตอน"
}
