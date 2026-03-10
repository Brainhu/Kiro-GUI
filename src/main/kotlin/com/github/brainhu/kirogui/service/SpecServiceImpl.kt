package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Project-level implementation of [SpecService].
 *
 * - Lists specs by scanning `.kiro/specs/` directory
 * - Creates new specs with directory structure and template files
 * - Parses `tasks.md` into [TaskItem] list with checkbox state detection
 * - Updates task status by toggling checkboxes in `tasks.md`
 * - Determines [SpecPhase] based on document existence and task completion
 *
 * Requirements: 3.1, 3.2, 3.3, 3.5, 3.6
 */
@Service(Service.Level.PROJECT)
class SpecServiceImpl(private val project: Project) : SpecService {

    private val log = Logger.getInstance(SpecServiceImpl::class.java)

    // ── Public API ──────────────────────────────────────────────────────

    override fun listSpecs(): List<SpecSummary> {
        val specsDir = getSpecsDir() ?: return emptyList()
        if (!specsDir.isDirectory) return emptyList()

        return specsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { dir ->
                val tasks = parseTasks(dir)
                val allTasks = flattenTasks(tasks)
                val completedCount = allTasks.count { it.completed }
                SpecSummary(
                    name = dir.name,
                    path = dir.absolutePath,
                    phase = determinePhase(dir, tasks),
                    taskProgress = TaskProgress(completed = completedCount, total = allTasks.size)
                )
            }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    override fun getSpec(specName: String): SpecDetail? {
        val specDir = getSpecDir(specName) ?: return null
        if (!specDir.isDirectory) return null

        val requirements = readFileContent(specDir, "requirements.md")
        val design = readFileContent(specDir, "design.md")
        val tasks = parseTasks(specDir)

        return SpecDetail(
            name = specName,
            requirements = requirements,
            design = design,
            tasks = tasks
        )
    }

    override fun createSpec(name: String): SpecDetail {
        val specsDir = getOrCreateSpecsDir()
        val specDir = File(specsDir, name)

        require(!specDir.exists()) { "Spec '$name' already exists" }
        specDir.mkdirs()

        // Create template files
        File(specDir, ".config.kiro").writeText(
            """{"specId": "${java.util.UUID.randomUUID()}", "workflowType": "requirements-first", "specType": "feature"}"""
        )
        File(specDir, "requirements.md").writeText("# Requirements: $name\n")
        File(specDir, "design.md").writeText("# Design: $name\n")
        File(specDir, "tasks.md").writeText("# Tasks: $name\n")

        log.info("Created spec: $name at ${specDir.absolutePath}")

        return SpecDetail(
            name = name,
            requirements = "# Requirements: $name\n",
            design = "# Design: $name\n",
            tasks = emptyList()
        )
    }

    override fun updateTaskStatus(specName: String, taskId: String, completed: Boolean) {
        val specDir = getSpecDir(specName)
            ?: throw IllegalArgumentException("Spec not found: $specName")
        val tasksFile = File(specDir, "tasks.md")
        if (!tasksFile.exists()) {
            throw IllegalArgumentException("tasks.md not found for spec: $specName")
        }

        val lines = tasksFile.readLines().toMutableList()
        val marker = if (completed) "[x]" else "[ ]"
        var found = false

        for (i in lines.indices) {
            val parsedId = extractTaskId(lines[i])
            if (parsedId == taskId) {
                lines[i] = lines[i].replaceFirst(Regex("\\[[ xX]]"), marker)
                found = true
                break
            }
        }

        if (!found) {
            throw IllegalArgumentException("Task not found: $taskId in spec: $specName")
        }

        tasksFile.writeText(lines.joinToString("\n"))
        log.info("Updated task $taskId in spec $specName to completed=$completed")
    }

    override fun getSpecPhase(specName: String): SpecPhase {
        val specDir = getSpecDir(specName)
            ?: return SpecPhase.REQUIREMENTS
        return determinePhase(specDir, parseTasks(specDir))
    }

    // ── Task Parsing ────────────────────────────────────────────────────

    companion object {
        /** Regex matching a task line: optional leading whitespace, `-`, `[x]` or `[ ]`, task id and description. */
        private val TASK_LINE_REGEX = Regex("""^(\s*)- \[([xX ])] (\S+)\s+(.*)$""")

        /**
         * Parse `tasks.md` content into a list of [TaskItem] with nested subtasks.
         * Exposed for testing.
         */
        fun parseTasksContent(content: String): List<TaskItem> {
            val lines = content.lines()
            return parseTaskLines(lines, 0).first
        }

        /**
         * Generate `tasks.md` content from a list of [TaskItem].
         * Exposed for testing.
         */
        fun generateTasksMarkdown(tasks: List<TaskItem>, indent: Int = 0): String {
            val sb = StringBuilder()
            val prefix = "  ".repeat(indent)
            for (task in tasks) {
                val checkbox = if (task.completed) "[x]" else "[ ]"
                sb.appendLine("$prefix- $checkbox ${task.id} ${task.description}")
                if (task.subtasks.isNotEmpty()) {
                    sb.append(generateTasksMarkdown(task.subtasks, indent + 1))
                }
            }
            return sb.toString()
        }

        /**
         * Toggle a specific task's status in the markdown content and return the updated content.
         * Exposed for testing.
         */
        fun toggleTaskInContent(content: String, taskId: String, completed: Boolean): String {
            val lines = content.lines().toMutableList()
            val marker = if (completed) "[x]" else "[ ]"
            for (i in lines.indices) {
                val parsedId = extractTaskId(lines[i])
                if (parsedId == taskId) {
                    lines[i] = lines[i].replaceFirst(Regex("\\[[ xX]]"), marker)
                    break
                }
            }
            return lines.joinToString("\n")
        }

        private fun parseTaskLines(lines: List<String>, baseIndent: Int): Pair<List<TaskItem>, Int> {
            val tasks = mutableListOf<TaskItem>()
            var i = 0

            while (i < lines.size) {
                val line = lines[i]
                val match = TASK_LINE_REGEX.matchEntire(line)
                if (match == null) {
                    i++
                    continue
                }

                val indent = match.groupValues[1].length
                if (indent < baseIndent) {
                    // We've gone back to a parent level — stop
                    break
                }
                if (indent > baseIndent) {
                    // This is a deeper level — skip, it was consumed by a parent
                    i++
                    continue
                }

                val checkMark = match.groupValues[2]
                val id = match.groupValues[3]
                val description = match.groupValues[4]
                val completed = checkMark.equals("x", ignoreCase = true)

                // Parse subtasks (lines at indent + 2)
                val remaining = lines.subList(i + 1, lines.size)
                val (subtasks, consumed) = parseTaskLines(remaining, baseIndent + 2)

                tasks.add(TaskItem(id = id, description = description, completed = completed, subtasks = subtasks))
                i += 1 + consumed
            }

            return tasks to (if (i > lines.size) lines.size else i)
        }

        private fun extractTaskId(line: String): String? {
            val match = TASK_LINE_REGEX.matchEntire(line) ?: return null
            return match.groupValues[3]
        }

        private fun flattenTasks(tasks: List<TaskItem>): List<TaskItem> {
            return tasks.flatMap { listOf(it) + flattenTasks(it.subtasks) }
        }
    }

    // ── Private Helpers ─────────────────────────────────────────────────

    private fun getSpecsDir(): File? {
        val basePath = project.basePath ?: return null
        return File(basePath, ".kiro/specs")
    }

    private fun getOrCreateSpecsDir(): File {
        val basePath = project.basePath
            ?: throw IllegalStateException("Project base path is not available")
        val dir = File(basePath, ".kiro/specs")
        dir.mkdirs()
        return dir
    }

    private fun getSpecDir(specName: String): File? {
        val specsDir = getSpecsDir() ?: return null
        val dir = File(specsDir, specName)
        return if (dir.isDirectory) dir else null
    }

    private fun readFileContent(specDir: File, fileName: String): String? {
        val file = File(specDir, fileName)
        return if (file.exists()) file.readText() else null
    }

    private fun parseTasks(specDir: File): List<TaskItem> {
        val tasksFile = File(specDir, "tasks.md")
        if (!tasksFile.exists()) return emptyList()
        return parseTasksContent(tasksFile.readText())
    }

    private fun determinePhase(specDir: File, tasks: List<TaskItem>): SpecPhase {
        val hasRequirements = File(specDir, "requirements.md").exists()
        val hasDesign = File(specDir, "design.md").exists()
        val hasTasksFile = File(specDir, "tasks.md").exists()

        if (!hasRequirements) return SpecPhase.REQUIREMENTS

        val allTasks = flattenTasks(tasks)
        if (hasTasksFile && allTasks.isNotEmpty() && allTasks.all { it.completed }) {
            return SpecPhase.COMPLETED
        }
        if (hasTasksFile && allTasks.isNotEmpty()) {
            return SpecPhase.TASKS
        }
        if (hasDesign) return SpecPhase.DESIGN

        return SpecPhase.REQUIREMENTS
    }
}
