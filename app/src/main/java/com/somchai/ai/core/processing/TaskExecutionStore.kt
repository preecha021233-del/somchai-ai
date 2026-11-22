package com.somchai.ai.core.processing

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class TaskStepStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED }

data class TaskStepRecord(
    val title: String,
    val status: TaskStepStatus,
    val failureReason: String? = null
)
data class TaskRecord(
    val id: String,
    val goal: String,
    val type: TaskType,
    val steps: List<TaskStepRecord>,
    val createdAt: String
)

/**
 * Durable task lifecycle tracking. This store records execution state only;
 * it never performs external side effects or marks work complete by itself.
 */
class TaskExecutionStore(private val directory: File) {
    private val file = File(directory, "tasks.json")
    private val lock = Any()

    init { check(directory.exists() || directory.mkdirs()) { "Cannot create task storage" } }

    fun create(plan: TaskPlan): TaskRecord = synchronized(lock) {
        require(plan.steps.isNotEmpty()) { "Task plan must contain at least one step" }
        require(plan.steps.size <= MAX_STEPS) { "Task plan has too many steps" }
        val record = TaskRecord(
            UUID.randomUUID().toString(), plan.goal.take(MAX_GOAL_CHARS), plan.type,
            plan.steps.map { TaskStepRecord(it.take(MAX_STEP_CHARS), TaskStepStatus.PENDING) },
            java.time.Instant.now().toString()
        )
        write(readAll() + record)
        record
    }

    fun get(id: String): TaskRecord? = synchronized(lock) {
        findById(readAll(), id)
    }

    fun all(): List<TaskRecord> = synchronized(lock) { readAll().asReversed() }

    fun startStep(id: String, index: Int): TaskRecord = updateTask(id) { current ->
        require(index in current.steps.indices) { "Task step index out of range" }
        require(current.steps[index].status == TaskStepStatus.PENDING) { "Task step is not pending" }
        require(current.steps.take(index).all { it.status == TaskStepStatus.COMPLETED }) {
            "Complete earlier task steps first"
        }
        require(current.steps.none { it.status == TaskStepStatus.IN_PROGRESS }) {
            "Another task step is already in progress"
        }
        current.copy(steps = current.steps.mapIndexed { i, step ->
            if (i == index) step.copy(status = TaskStepStatus.IN_PROGRESS, failureReason = null) else step
        })
    }

    fun completeStep(id: String, index: Int): TaskRecord = updateTask(id) { current ->
        require(index in current.steps.indices) { "Task step index out of range" }
        require(current.steps[index].status == TaskStepStatus.IN_PROGRESS) {
            "Only an in-progress step can be completed"
        }
        current.copy(steps = current.steps.mapIndexed { i, step ->
            if (i == index) step.copy(status = TaskStepStatus.COMPLETED, failureReason = null) else step
        })
    }

    /** Records a failure without silently completing or deleting the task. */
    fun failStep(id: String, index: Int, reason: String): TaskRecord = updateTask(id) { current ->
        require(index in current.steps.indices) { "Task step index out of range" }
        require(current.steps[index].status == TaskStepStatus.IN_PROGRESS) {
            "Only an in-progress step can fail"
        }
        val safeReason = reason.trim().take(MAX_FAILURE_CHARS).ifBlank { "ไม่ทราบสาเหตุ" }
        current.copy(steps = current.steps.mapIndexed { i, step ->
            if (i == index) step.copy(status = TaskStepStatus.FAILED, failureReason = safeReason) else step
        })
    }

    /** Makes a failed step eligible for a deliberate retry. */
    fun retryStep(id: String, index: Int): TaskRecord = updateTask(id) { current ->
        require(index in current.steps.indices) { "Task step index out of range" }
        require(current.steps[index].status == TaskStepStatus.FAILED) { "Only a failed step can be retried" }
        require(current.steps.take(index).all { it.status == TaskStepStatus.COMPLETED }) {
            "Earlier task steps must be completed before retry"
        }
        current.copy(steps = current.steps.mapIndexed { i, step ->
            if (i == index) step.copy(status = TaskStepStatus.PENDING, failureReason = null) else step
        })
    }

    /** Cancels a pending or active step; completed work is immutable. */
    fun cancelStep(id: String, index: Int): TaskRecord = updateTask(id) { current ->
        require(index in current.steps.indices) { "Task step index out of range" }
        val status = current.steps[index].status
        require(status == TaskStepStatus.PENDING || status == TaskStepStatus.IN_PROGRESS) {
            "Only pending or in-progress steps can be cancelled"
        }
        current.copy(steps = current.steps.mapIndexed { i, step ->
            if (i == index) step.copy(status = TaskStepStatus.CANCELLED, failureReason = null) else step
        })
    }

    private fun updateTask(id: String, transform: (TaskRecord) -> TaskRecord): TaskRecord = synchronized(lock) {
        val records = readAll().toMutableList()
        val matched = findById(records, id) ?: error("Task not found")
        val position = records.indexOfFirst { it.id == matched.id }
        val updated = transform(records[position])
        records[position] = updated
        write(records)
        updated
    }

    private fun findById(records: List<TaskRecord>, id: String): TaskRecord? {
        require(id.isNotBlank()) { "Task ID cannot be blank" }
        val matches = records.filter { it.id == id || it.id.startsWith(id) }
        require(matches.size <= 1) { "Task ID prefix is ambiguous" }
        return matches.firstOrNull()
    }

    private fun readAll(): List<TaskRecord> {
        if (!file.isFile) return emptyList()
        require(file.length() <= MAX_FILE_BYTES) { "Task store exceeds size limit" }
        val array = JSONArray(file.readText())
        require(array.length() <= MAX_TASKS) { "Task store has too many records" }
        return (0 until array.length()).map { i ->
            val json = array.getJSONObject(i)
            val steps = json.getJSONArray("steps")
            require(steps.length() in 1..MAX_STEPS) { "Invalid task step count" }
            val record = TaskRecord(
                json.getString("id"), json.getString("goal"),
                TaskType.valueOf(json.getString("type")),
                (0 until steps.length()).map { j ->
                    val step = steps.getJSONObject(j)
                    TaskStepRecord(
                        step.getString("title"),
                        TaskStepStatus.valueOf(step.getString("status")),
                        step.optString("failureReason").takeIf { it.isNotBlank() && it != "null" }
                    )
                },
                json.getString("createdAt")
            )
            require(runCatching { UUID.fromString(record.id) }.isSuccess) { "Invalid task ID" }
            require(record.goal.isNotBlank() && record.goal.length <= MAX_GOAL_CHARS) { "Invalid task goal" }
            require(runCatching { java.time.Instant.parse(record.createdAt) }.isSuccess) { "Invalid task timestamp" }
            record.steps.forEach { step ->
                require(step.title.isNotBlank() && step.title.length <= MAX_STEP_CHARS) { "Invalid task step" }
                if (step.status == TaskStepStatus.FAILED) {
                    require(!step.failureReason.isNullOrBlank()) { "Failed task step has no reason" }
                }
            }
            record
        }.also { records ->
            require(records.map { it.id }.distinct().size == records.size) { "Duplicate task IDs" }
        }
    }

    private fun write(records: List<TaskRecord>) {
        require(records.size <= MAX_TASKS) { "Task store has too many records" }
        val array = JSONArray()
        records.forEach { record ->
            val steps = JSONArray()
            record.steps.forEach { steps.put(JSONObject()
                .put("title", it.title).put("status", it.status.name)
                .put("failureReason", it.failureReason)) }
            array.put(JSONObject()
                .put("id", record.id).put("goal", record.goal)
                .put("type", record.type.name).put("steps", steps)
                .put("createdAt", record.createdAt))
        }
        val encoded = array.toString()
        require(encoded.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "Task store exceeds size limit" }
        val temp = File(directory, "tasks.json.tmp")
        temp.writeText(encoded)
        check(temp.renameTo(file) || run {
            temp.copyTo(file, overwrite = true)
            temp.delete()
            true
        }) { "Could not save task records" }
    }

    companion object {
        private const val MAX_TASKS = 500
        private const val MAX_STEPS = 30
        private const val MAX_GOAL_CHARS = 1000
        private const val MAX_STEP_CHARS = 500
        private const val MAX_FAILURE_CHARS = 500
        private const val MAX_FILE_BYTES = 2_000_000L
    }
}
