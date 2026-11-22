package com.somchai.ai.core.processing

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class TaskStepStatus { PENDING, IN_PROGRESS, COMPLETED }

data class TaskStepRecord(val title: String, val status: TaskStepStatus)
data class TaskRecord(
    val id: String,
    val goal: String,
    val type: TaskType,
    val steps: List<TaskStepRecord>,
    val createdAt: String
)

/** Durable task tracking. Starting a step records intent/status; it does not perform external side effects. */
class TaskExecutionStore(private val directory: File) {
    private val file = File(directory, "tasks.json")
    private val lock = Any()

    init { check(directory.exists() || directory.mkdirs()) { "Cannot create task storage" } }

    fun create(plan: TaskPlan): TaskRecord = synchronized(lock) {
        require(plan.steps.isNotEmpty()) { "Task plan must contain at least one step" }
        val record = TaskRecord(
            UUID.randomUUID().toString(), plan.goal, plan.type,
            plan.steps.map { TaskStepRecord(it, TaskStepStatus.PENDING) },
            java.time.Instant.now().toString()
        )
        write(readAll() + record)
        record
    }

    fun get(id: String): TaskRecord? = synchronized(lock) {
        readAll().firstOrNull { it.id == id || it.id.startsWith(id) }
    }

    fun all(): List<TaskRecord> = synchronized(lock) { readAll().asReversed() }

    fun startStep(id: String, index: Int): TaskRecord = synchronized(lock) {
        val records = readAll().toMutableList()
        val position = records.indexOfFirst { it.id == id || it.id.startsWith(id) }
        require(position >= 0) { "Task not found" }
        val current = records[position]
        require(index in current.steps.indices) { "Task step index out of range" }
        require(current.steps[index].status == TaskStepStatus.PENDING) { "Task step is not pending" }
        require(current.steps.take(index).all { it.status == TaskStepStatus.COMPLETED }) {
            "Complete earlier task steps first"
        }
        require(current.steps.none { it.status == TaskStepStatus.IN_PROGRESS }) {
            "Another task step is already in progress"
        }
        val updated = current.copy(steps = current.steps.mapIndexed { i, step ->
            if (i == index) step.copy(status = TaskStepStatus.IN_PROGRESS) else step
        })
        records[position] = updated
        write(records)
        updated
    }

    fun completeStep(id: String, index: Int): TaskRecord = synchronized(lock) {
        val records = readAll().toMutableList()
        val position = records.indexOfFirst { it.id == id || it.id.startsWith(id) }
        require(position >= 0) { "Task not found" }
        val current = records[position]
        require(index in current.steps.indices) { "Task step index out of range" }
        require(current.steps[index].status == TaskStepStatus.IN_PROGRESS) {
            "Only an in-progress step can be completed"
        }
        val updated = current.copy(steps = current.steps.mapIndexed { i, step ->
            if (i == index) step.copy(status = TaskStepStatus.COMPLETED) else step
        })
        records[position] = updated
        write(records)
        updated
    }

    private fun readAll(): List<TaskRecord> {
        if (!file.isFile) return emptyList()
        val array = JSONArray(file.readText())
        return (0 until array.length()).map { i ->
            val json = array.getJSONObject(i)
            val steps = json.getJSONArray("steps")
            TaskRecord(
                json.getString("id"), json.getString("goal"),
                TaskType.valueOf(json.getString("type")),
                (0 until steps.length()).map { j ->
                    val step = steps.getJSONObject(j)
                    TaskStepRecord(step.getString("title"), TaskStepStatus.valueOf(step.getString("status")))
                },
                json.getString("createdAt")
            )
        }
    }

    private fun write(records: List<TaskRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            val steps = JSONArray()
            record.steps.forEach { steps.put(JSONObject()
                .put("title", it.title).put("status", it.status.name)) }
            array.put(JSONObject()
                .put("id", record.id).put("goal", record.goal)
                .put("type", record.type.name).put("steps", steps)
                .put("createdAt", record.createdAt))
        }
        val temp = File(directory, "tasks.json.tmp")
        temp.writeText(array.toString())
        check(temp.renameTo(file) || run {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }) { "Could not save task records" }
    }
}
