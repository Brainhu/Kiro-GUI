package com.github.brainhu.kirogui.model

data class SpecSummary(
    val name: String,
    val path: String,
    val phase: SpecPhase,
    val taskProgress: TaskProgress
)

data class SpecDetail(
    val name: String,
    val requirements: String?,
    val design: String?,
    val tasks: List<TaskItem>
)

data class TaskItem(
    val id: String,
    val description: String,
    val completed: Boolean,
    val subtasks: List<TaskItem> = emptyList()
)

enum class SpecPhase {
    REQUIREMENTS, DESIGN, TASKS, COMPLETED
}

data class TaskProgress(
    val completed: Int,
    val total: Int
)
