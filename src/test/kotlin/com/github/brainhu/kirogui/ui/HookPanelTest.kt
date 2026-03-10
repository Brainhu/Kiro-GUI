package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.HookConfig
import com.github.brainhu.kirogui.model.HookExecutionRecord
import com.github.brainhu.kirogui.model.HookTrigger
import com.github.brainhu.kirogui.service.HookService
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests for HookPanel.
 * 
 * Requirements: 5.3, 5.4
 */
class HookPanelTest {

    @Test
    fun testHookPanelCreation() {
        val project = mockk<Project>(relaxed = true)
        val hookService = mockk<HookService>(relaxed = true)
        
        every { project.getService(HookService::class.java) } returns hookService
        every { hookService.listHooks() } returns emptyList()
        every { hookService.getExecutionLog() } returns emptyList()
        
        // Should not throw exception
        assertDoesNotThrow {
            HookPanel(project)
        }
    }

    @Test
    fun testHookPanelLoadsHooks() {
        val project = mockk<Project>(relaxed = true)
        val hookService = mockk<HookService>(relaxed = true)
        
        val hooks = listOf(
            HookConfig(
                id = "hook1",
                name = "Format on Save",
                trigger = HookTrigger.FILE_SAVE,
                enabled = true,
                filePath = ".kiro/hooks/hook1.json"
            ),
            HookConfig(
                id = "hook2",
                name = "Pre-commit Lint",
                trigger = HookTrigger.PRE_COMMIT,
                enabled = false,
                filePath = ".kiro/hooks/hook2.json"
            )
        )
        
        every { project.getService(HookService::class.java) } returns hookService
        every { hookService.listHooks() } returns hooks
        every { hookService.getExecutionLog() } returns emptyList()
        
        // Should not throw exception
        assertDoesNotThrow {
            val panel = HookPanel(project)
            verify { hookService.listHooks() }
        }
    }

    @Test
    fun testHookPanelLoadsExecutionLog() {
        val project = mockk<Project>(relaxed = true)
        val hookService = mockk<HookService>(relaxed = true)
        
        val logs = listOf(
            HookExecutionRecord(
                hookId = "hook1",
                timestamp = Instant.now(),
                success = true,
                message = "Hook executed successfully",
                triggerFile = "src/Main.kt"
            ),
            HookExecutionRecord(
                hookId = "hook2",
                timestamp = Instant.now(),
                success = false,
                message = "Hook execution failed",
                triggerFile = "src/Test.kt"
            )
        )
        
        every { project.getService(HookService::class.java) } returns hookService
        every { hookService.listHooks() } returns emptyList()
        every { hookService.getExecutionLog() } returns logs
        
        // Should not throw exception
        assertDoesNotThrow {
            val panel = HookPanel(project)
            verify { hookService.getExecutionLog() }
        }
    }

    @Test
    fun testHookPanelRefresh() {
        val project = mockk<Project>(relaxed = true)
        val hookService = mockk<HookService>(relaxed = true)
        
        every { project.getService(HookService::class.java) } returns hookService
        every { hookService.listHooks() } returns emptyList()
        every { hookService.getExecutionLog() } returns emptyList()
        
        val panel = HookPanel(project)
        
        // Should not throw exception
        assertDoesNotThrow {
            panel.refresh()
        }
        
        // Verify service methods were called
        verify(atLeast = 2) { hookService.listHooks() }
        verify(atLeast = 2) { hookService.getExecutionLog() }
    }

    @Test
    fun testHookPanelWithMultipleHooksAndLogs() {
        val project = mockk<Project>(relaxed = true)
        val hookService = mockk<HookService>(relaxed = true)
        
        val hooks = listOf(
            HookConfig("hook1", "Hook 1", HookTrigger.FILE_SAVE, true, "path1"),
            HookConfig("hook2", "Hook 2", HookTrigger.FILE_CREATE, false, "path2"),
            HookConfig("hook3", "Hook 3", HookTrigger.FILE_DELETE, true, "path3")
        )
        
        val logs = listOf(
            HookExecutionRecord("hook1", Instant.now(), true, "Success 1", "file1.kt"),
            HookExecutionRecord("hook2", Instant.now(), false, "Failed 2", "file2.kt"),
            HookExecutionRecord("hook3", Instant.now(), true, "Success 3", "file3.kt")
        )
        
        every { project.getService(HookService::class.java) } returns hookService
        every { hookService.listHooks() } returns hooks
        every { hookService.getExecutionLog() } returns logs
        
        // Should not throw exception
        assertDoesNotThrow {
            HookPanel(project)
        }
    }
}
