package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.SpecPhase
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Unit tests for [SpecServiceImpl] covering spec listing, creation,
 * task parsing, task status updates, and phase determination.
 *
 * Requirements: 3.1, 3.2, 3.3, 3.5, 3.6
 */
class SpecServiceImplTest : BasePlatformTestCase() {

    private lateinit var specService: SpecServiceImpl
    private lateinit var specsDir: File

    override fun setUp() {
        super.setUp()
        specService = SpecServiceImpl(project)
        specsDir = File(project.basePath!!, ".kiro/specs")
        // Clean up any pre-existing specs directory to ensure test isolation
        if (specsDir.exists()) {
            specsDir.deleteRecursively()
        }
        specsDir.mkdirs()
    }

    // ── listSpecs ───────────────────────────────────────────────────────

    fun `test listSpecs returns empty when no specs exist`() {
        val specs = specService.listSpecs()
        assertTrue(specs.isEmpty())
    }

    fun `test listSpecs returns specs sorted by name`() {
        createSpecDir("beta-feature")
        createSpecDir("alpha-feature")

        val specs = specService.listSpecs()
        assertEquals(2, specs.size)
        assertEquals("alpha-feature", specs[0].name)
        assertEquals("beta-feature", specs[1].name)
    }

    fun `test listSpecs computes task progress`() {
        val dir = createSpecDir("my-spec")
        File(dir, "tasks.md").writeText(
            """
            # Tasks
            - [x] 1.1 Done task
            - [ ] 1.2 Pending task
            - [x] 1.3 Another done
            """.trimIndent()
        )

        val specs = specService.listSpecs()
        assertEquals(1, specs.size)
        assertEquals(2, specs[0].taskProgress.completed)
        assertEquals(3, specs[0].taskProgress.total)
    }

    // ── getSpec ─────────────────────────────────────────────────────────

    fun `test getSpec returns null for nonexistent spec`() {
        assertNull(specService.getSpec("nonexistent"))
    }

    fun `test getSpec returns detail with all documents`() {
        val dir = createSpecDir("my-spec")
        File(dir, "requirements.md").writeText("# Requirements\nSome reqs")
        File(dir, "design.md").writeText("# Design\nSome design")
        File(dir, "tasks.md").writeText("- [x] 1.1 Task one\n- [ ] 1.2 Task two")

        val detail = specService.getSpec("my-spec")
        assertNotNull(detail)
        assertEquals("my-spec", detail!!.name)
        assertTrue(detail.requirements!!.contains("Some reqs"))
        assertTrue(detail.design!!.contains("Some design"))
        assertEquals(2, detail.tasks.size)
    }

    fun `test getSpec returns null fields for missing documents`() {
        createSpecDir("empty-spec")

        val detail = specService.getSpec("empty-spec")
        assertNotNull(detail)
        assertNull(detail!!.requirements)
        assertNull(detail.design)
        assertTrue(detail.tasks.isEmpty())
    }

    // ── createSpec ──────────────────────────────────────────────────────

    fun `test createSpec creates directory structure with all files`() {
        val detail = specService.createSpec("new-feature")

        assertEquals("new-feature", detail.name)
        assertNotNull(detail.requirements)
        assertNotNull(detail.design)
        assertTrue(detail.tasks.isEmpty())

        val specDir = File(specsDir, "new-feature")
        assertTrue(specDir.isDirectory)
        assertTrue(File(specDir, ".config.kiro").exists())
        assertTrue(File(specDir, "requirements.md").exists())
        assertTrue(File(specDir, "design.md").exists())
        assertTrue(File(specDir, "tasks.md").exists())
    }

    fun `test createSpec config file contains valid JSON`() {
        specService.createSpec("json-spec")

        val configContent = File(specsDir, "json-spec/.config.kiro").readText()
        assertTrue(configContent.contains("specId"))
        assertTrue(configContent.contains("requirements-first"))
        assertTrue(configContent.contains("feature"))
    }

    fun `test createSpec throws for duplicate name`() {
        specService.createSpec("dup-spec")
        assertThrows(IllegalArgumentException::class.java) {
            specService.createSpec("dup-spec")
        }
    }

    // ── Task Parsing ────────────────────────────────────────────────────

    fun `test parseTasksContent parses simple tasks`() {
        val content = """
            - [x] 1.1 Completed task
            - [ ] 1.2 Incomplete task
        """.trimIndent()

        val tasks = SpecServiceImpl.parseTasksContent(content)
        assertEquals(2, tasks.size)
        assertEquals("1.1", tasks[0].id)
        assertEquals("Completed task", tasks[0].description)
        assertTrue(tasks[0].completed)
        assertEquals("1.2", tasks[1].id)
        assertFalse(tasks[1].completed)
    }

    fun `test parseTasksContent parses nested subtasks`() {
        val content = """
            - [x] 1.1 Parent task
              - [x] 1.1.1 Subtask one
              - [ ] 1.1.2 Subtask two
            - [ ] 1.2 Another task
        """.trimIndent()

        val tasks = SpecServiceImpl.parseTasksContent(content)
        assertEquals(2, tasks.size)
        assertEquals(2, tasks[0].subtasks.size)
        assertEquals("1.1.1", tasks[0].subtasks[0].id)
        assertTrue(tasks[0].subtasks[0].completed)
        assertEquals("1.1.2", tasks[0].subtasks[1].id)
        assertFalse(tasks[0].subtasks[1].completed)
    }

    fun `test parseTasksContent ignores non-task lines`() {
        val content = """
            # Tasks
            
            Some description text
            
            - [x] 1.1 A real task
        """.trimIndent()

        val tasks = SpecServiceImpl.parseTasksContent(content)
        assertEquals(1, tasks.size)
        assertEquals("1.1", tasks[0].id)
    }

    fun `test parseTasksContent handles empty content`() {
        val tasks = SpecServiceImpl.parseTasksContent("")
        assertTrue(tasks.isEmpty())
    }

    fun `test parseTasksContent handles uppercase X`() {
        val content = "- [X] 1.1 Task with uppercase X"
        val tasks = SpecServiceImpl.parseTasksContent(content)
        assertEquals(1, tasks.size)
        assertTrue(tasks[0].completed)
    }

    // ── updateTaskStatus ────────────────────────────────────────────────

    fun `test updateTaskStatus marks task as completed`() {
        val dir = createSpecDir("update-spec")
        File(dir, "tasks.md").writeText("- [ ] 1.1 My task\n- [ ] 1.2 Other task")

        specService.updateTaskStatus("update-spec", "1.1", true)

        val detail = specService.getSpec("update-spec")!!
        assertTrue(detail.tasks[0].completed)
        assertFalse(detail.tasks[1].completed)
    }

    fun `test updateTaskStatus marks task as incomplete`() {
        val dir = createSpecDir("update-spec2")
        File(dir, "tasks.md").writeText("- [x] 1.1 My task\n- [x] 1.2 Other task")

        specService.updateTaskStatus("update-spec2", "1.1", false)

        val detail = specService.getSpec("update-spec2")!!
        assertFalse(detail.tasks[0].completed)
        assertTrue(detail.tasks[1].completed)
    }

    fun `test updateTaskStatus does not affect other tasks`() {
        val dir = createSpecDir("isolate-spec")
        File(dir, "tasks.md").writeText(
            "- [ ] 1.1 Task A\n- [x] 1.2 Task B\n- [ ] 1.3 Task C"
        )

        specService.updateTaskStatus("isolate-spec", "1.2", false)

        val detail = specService.getSpec("isolate-spec")!!
        assertFalse(detail.tasks[0].completed) // unchanged
        assertFalse(detail.tasks[1].completed) // toggled
        assertFalse(detail.tasks[2].completed) // unchanged
    }

    fun `test updateTaskStatus throws for nonexistent spec`() {
        assertThrows(IllegalArgumentException::class.java) {
            specService.updateTaskStatus("nonexistent", "1.1", true)
        }
    }

    fun `test updateTaskStatus throws for nonexistent task`() {
        val dir = createSpecDir("missing-task-spec")
        File(dir, "tasks.md").writeText("- [ ] 1.1 Only task")

        assertThrows(IllegalArgumentException::class.java) {
            specService.updateTaskStatus("missing-task-spec", "9.9", true)
        }
    }

    // ── getSpecPhase ────────────────────────────────────────────────────

    fun `test getSpecPhase returns REQUIREMENTS when no docs exist`() {
        createSpecDir("phase-empty")
        assertEquals(SpecPhase.REQUIREMENTS, specService.getSpecPhase("phase-empty"))
    }

    fun `test getSpecPhase returns REQUIREMENTS when only requirements exists`() {
        val dir = createSpecDir("phase-req")
        File(dir, "requirements.md").writeText("# Reqs")
        assertEquals(SpecPhase.REQUIREMENTS, specService.getSpecPhase("phase-req"))
    }

    fun `test getSpecPhase returns DESIGN when requirements and design exist`() {
        val dir = createSpecDir("phase-design")
        File(dir, "requirements.md").writeText("# Reqs")
        File(dir, "design.md").writeText("# Design")
        assertEquals(SpecPhase.DESIGN, specService.getSpecPhase("phase-design"))
    }

    fun `test getSpecPhase returns TASKS when tasks have incomplete items`() {
        val dir = createSpecDir("phase-tasks")
        File(dir, "requirements.md").writeText("# Reqs")
        File(dir, "design.md").writeText("# Design")
        File(dir, "tasks.md").writeText("- [x] 1.1 Done\n- [ ] 1.2 Not done")
        assertEquals(SpecPhase.TASKS, specService.getSpecPhase("phase-tasks"))
    }

    fun `test getSpecPhase returns COMPLETED when all tasks are done`() {
        val dir = createSpecDir("phase-done")
        File(dir, "requirements.md").writeText("# Reqs")
        File(dir, "design.md").writeText("# Design")
        File(dir, "tasks.md").writeText("- [x] 1.1 Done\n- [x] 1.2 Also done")
        assertEquals(SpecPhase.COMPLETED, specService.getSpecPhase("phase-done"))
    }

    fun `test getSpecPhase returns REQUIREMENTS for nonexistent spec`() {
        assertEquals(SpecPhase.REQUIREMENTS, specService.getSpecPhase("nonexistent"))
    }

    // ── Static helpers (generateTasksMarkdown, toggleTaskInContent) ─────

    fun `test generateTasksMarkdown produces correct format`() {
        val content = SpecServiceImpl.generateTasksMarkdown(
            listOf(
                com.github.brainhu.kirogui.model.TaskItem("1.1", "Task A", true),
                com.github.brainhu.kirogui.model.TaskItem("1.2", "Task B", false)
            )
        )
        assertTrue(content.contains("- [x] 1.1 Task A"))
        assertTrue(content.contains("- [ ] 1.2 Task B"))
    }

    fun `test toggleTaskInContent toggles correct task`() {
        val original = "- [ ] 1.1 Task A\n- [x] 1.2 Task B"
        val updated = SpecServiceImpl.toggleTaskInContent(original, "1.1", true)
        assertTrue(updated.contains("- [x] 1.1 Task A"))
        assertTrue(updated.contains("- [x] 1.2 Task B"))
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun createSpecDir(name: String): File {
        val dir = File(specsDir, name)
        dir.mkdirs()
        return dir
    }
}
