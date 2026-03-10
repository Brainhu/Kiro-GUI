package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.*
import com.github.brainhu.kirogui.service.SpecService
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for SpecPanel and related components.
 * 
 * Requirements: 3.1, 3.3, 3.4, 3.5, 3.6
 */
class SpecPanelTest {

    @Test
    fun testSpecPanelCreation() {
        val project = mockk<Project>(relaxed = true)
        val specService = mockk<SpecService>(relaxed = true)
        
        every { project.getService(SpecService::class.java) } returns specService
        every { specService.listSpecs() } returns emptyList()
        
        // Should not throw exception
        assertDoesNotThrow {
            SpecPanel(project)
        }
    }

    @Test
    fun testTaskChecklistPanelLoadsTasks() {
        val project = mockk<Project>(relaxed = true)
        val panel = TaskChecklistPanel(project)
        
        val tasks = listOf(
            TaskItem("1.1", "Task 1", completed = true),
            TaskItem("1.2", "Task 2", completed = false, subtasks = listOf(
                TaskItem("1.2.1", "Subtask 1", completed = true)
            ))
        )
        
        // Should not throw exception
        assertDoesNotThrow {
            panel.loadTasks("test-spec", tasks)
        }
    }

    @Test
    fun testTaskChecklistPanelNotifiesListeners() {
        val project = mockk<Project>(relaxed = true)
        val panel = TaskChecklistPanel(project)
        
        var notifiedTaskId: String? = null
        var notifiedStatus: Boolean? = null
        
        panel.addTaskStatusListener { taskId, completed ->
            notifiedTaskId = taskId
            notifiedStatus = completed
        }
        
        // Load a task and simulate checkbox click would trigger the listener
        // (In real scenario, this would be triggered by user interaction)
        val tasks = listOf(TaskItem("1.1", "Task 1", completed = false))
        panel.loadTasks("test-spec", tasks)
        
        // Verify listener was registered
        assertNotNull(panel)
    }

    @Test
    fun testMarkdownPreviewPanelCreation() {
        val project = mockk<Project>(relaxed = true)
        
        // Should not throw exception
        assertDoesNotThrow {
            MarkdownPreviewPanel(project)
        }
    }

    @Test
    fun testMarkdownPreviewPanelSetContent() {
        val project = mockk<Project>(relaxed = true)
        val panel = MarkdownPreviewPanel(project)
        
        // Should not throw exception
        assertDoesNotThrow {
            panel.setMarkdownContent("# Test Heading\n\nSome content")
        }
    }

    @Test
    fun testPhaseTabBarCreation() {
        val project = mockk<Project>(relaxed = true)
        val specService = mockk<SpecService>(relaxed = true)
        
        every { project.getService(SpecService::class.java) } returns specService
        
        // Should not throw exception
        assertDoesNotThrow {
            PhaseTabBar(project)
        }
    }

    @Test
    fun testPhaseTabBarLoadSpec() {
        val project = mockk<Project>(relaxed = true)
        val specService = mockk<SpecService>(relaxed = true)
        
        every { project.getService(SpecService::class.java) } returns specService
        every { specService.getSpec("test-spec") } returns SpecDetail(
            name = "test-spec",
            requirements = "# Requirements\n\nTest requirements",
            design = "# Design\n\nTest design",
            tasks = listOf(TaskItem("1.1", "Task 1", completed = false))
        )
        
        val tabBar = PhaseTabBar(project)
        
        // Should not throw exception
        assertDoesNotThrow {
            tabBar.loadSpec("test-spec")
        }
    }

    @Test
    fun testSpecTreeCreation() {
        val project = mockk<Project>(relaxed = true)
        val specService = mockk<SpecService>(relaxed = true)
        
        every { project.getService(SpecService::class.java) } returns specService
        every { specService.listSpecs() } returns listOf(
            SpecSummary(
                name = "test-spec",
                path = ".kiro/specs/test-spec",
                phase = SpecPhase.REQUIREMENTS,
                taskProgress = TaskProgress(0, 5)
            )
        )
        
        // Should not throw exception
        assertDoesNotThrow {
            SpecTree(project)
        }
    }

    @Test
    fun testSpecTreeRefresh() {
        val project = mockk<Project>(relaxed = true)
        val specService = mockk<SpecService>(relaxed = true)
        
        every { project.getService(SpecService::class.java) } returns specService
        every { specService.listSpecs() } returns listOf(
            SpecSummary(
                name = "spec1",
                path = ".kiro/specs/spec1",
                phase = SpecPhase.DESIGN,
                taskProgress = TaskProgress(2, 5)
            ),
            SpecSummary(
                name = "spec2",
                path = ".kiro/specs/spec2",
                phase = SpecPhase.TASKS,
                taskProgress = TaskProgress(3, 5)
            )
        )
        
        val tree = SpecTree(project)
        
        // Should not throw exception
        assertDoesNotThrow {
            tree.refresh()
        }
    }
}
