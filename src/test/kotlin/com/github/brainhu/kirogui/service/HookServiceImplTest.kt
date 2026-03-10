package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.HookConfig
import com.github.brainhu.kirogui.model.HookTrigger
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Unit tests for [HookServiceImpl] covering hook listing, config parsing,
 * enable/disable toggle with persistence, event listener registration,
 * hook execution recording, and error handling.
 *
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
class HookServiceImplTest : BasePlatformTestCase() {

    private lateinit var hookService: HookServiceImpl
    private lateinit var hooksDir: File
    private val json = Json { prettyPrint = true }

    override fun setUp() {
        super.setUp()
        hookService = HookServiceImpl(project)
        hooksDir = File(project.basePath!!, ".kiro/hooks")
        if (hooksDir.exists()) {
            hooksDir.deleteRecursively()
        }
        hooksDir.mkdirs()
    }

    override fun tearDown() {
        try {
            hookService.unregisterEventListeners()
        } finally {
            super.tearDown()
        }
    }

    // ── listHooks ───────────────────────────────────────────────────────

    fun `test listHooks returns empty when no hooks exist`() {
        val hooks = hookService.listHooks()
        assertTrue(hooks.isEmpty())
    }

    fun `test listHooks returns empty when hooks dir does not exist`() {
        hooksDir.deleteRecursively()
        val hooks = hookService.listHooks()
        assertTrue(hooks.isEmpty())
    }

    fun `test listHooks returns parsed hook configs sorted by id`() {
        writeHookConfig("beta-hook", HookConfig("beta-hook", "Beta Hook", HookTrigger.FILE_SAVE, true, "beta.json"))
        writeHookConfig("alpha-hook", HookConfig("alpha-hook", "Alpha Hook", HookTrigger.FILE_CREATE, false, "alpha.json"))

        val hooks = hookService.listHooks()
        assertEquals(2, hooks.size)
        assertEquals("alpha-hook", hooks[0].id)
        assertEquals("beta-hook", hooks[1].id)
    }

    fun `test listHooks ignores non-json files`() {
        writeHookConfig("valid-hook", HookConfig("valid-hook", "Valid", HookTrigger.FILE_SAVE, true, "valid.json"))
        File(hooksDir, "readme.md").writeText("# Not a hook")
        File(hooksDir, "notes.txt").writeText("Some notes")

        val hooks = hookService.listHooks()
        assertEquals(1, hooks.size)
        assertEquals("valid-hook", hooks[0].id)
    }

    fun `test listHooks skips malformed json files`() {
        writeHookConfig("good-hook", HookConfig("good-hook", "Good", HookTrigger.FILE_SAVE, true, "good.json"))
        File(hooksDir, "bad-hook.json").writeText("{ invalid json }")

        val hooks = hookService.listHooks()
        assertEquals(1, hooks.size)
        assertEquals("good-hook", hooks[0].id)
    }

    fun `test listHooks returns correct trigger types`() {
        writeHookConfig("save-hook", HookConfig("save-hook", "Save", HookTrigger.FILE_SAVE, true, "save.json"))
        writeHookConfig("create-hook", HookConfig("create-hook", "Create", HookTrigger.FILE_CREATE, true, "create.json"))
        writeHookConfig("delete-hook", HookConfig("delete-hook", "Delete", HookTrigger.FILE_DELETE, false, "delete.json"))
        writeHookConfig("commit-hook", HookConfig("commit-hook", "Commit", HookTrigger.PRE_COMMIT, true, "commit.json"))

        val hooks = hookService.listHooks()
        assertEquals(4, hooks.size)
        assertEquals(HookTrigger.PRE_COMMIT, hooks.first { it.id == "commit-hook" }.trigger)
        assertEquals(HookTrigger.FILE_CREATE, hooks.first { it.id == "create-hook" }.trigger)
        assertEquals(HookTrigger.FILE_DELETE, hooks.first { it.id == "delete-hook" }.trigger)
        assertEquals(HookTrigger.FILE_SAVE, hooks.first { it.id == "save-hook" }.trigger)
    }

    // ── setHookEnabled ──────────────────────────────────────────────────

    fun `test setHookEnabled enables a disabled hook`() {
        writeHookConfig("my-hook", HookConfig("my-hook", "My Hook", HookTrigger.FILE_SAVE, false, "my-hook.json"))

        hookService.setHookEnabled("my-hook", true)

        val hooks = hookService.listHooks()
        assertEquals(1, hooks.size)
        assertTrue(hooks[0].enabled)
    }

    fun `test setHookEnabled disables an enabled hook`() {
        writeHookConfig("my-hook", HookConfig("my-hook", "My Hook", HookTrigger.FILE_SAVE, true, "my-hook.json"))

        hookService.setHookEnabled("my-hook", false)

        val hooks = hookService.listHooks()
        assertEquals(1, hooks.size)
        assertFalse(hooks[0].enabled)
    }

    fun `test setHookEnabled persists change to file`() {
        writeHookConfig("persist-hook", HookConfig("persist-hook", "Persist", HookTrigger.FILE_CREATE, false, "persist-hook.json"))

        hookService.setHookEnabled("persist-hook", true)

        // Re-read from disk directly
        val fileContent = File(hooksDir, "persist-hook.json").readText()
        val reloaded = json.decodeFromString(HookConfig.serializer(), fileContent)
        assertTrue(reloaded.enabled)
        assertEquals("persist-hook", reloaded.id)
        assertEquals("Persist", reloaded.name)
        assertEquals(HookTrigger.FILE_CREATE, reloaded.trigger)
    }

    fun `test setHookEnabled does not affect other hooks`() {
        writeHookConfig("hook-a", HookConfig("hook-a", "A", HookTrigger.FILE_SAVE, true, "hook-a.json"))
        writeHookConfig("hook-b", HookConfig("hook-b", "B", HookTrigger.FILE_CREATE, false, "hook-b.json"))

        hookService.setHookEnabled("hook-b", true)

        val hooks = hookService.listHooks()
        val hookA = hooks.first { it.id == "hook-a" }
        val hookB = hooks.first { it.id == "hook-b" }
        assertTrue(hookA.enabled)  // unchanged
        assertTrue(hookB.enabled)  // toggled
    }

    fun `test setHookEnabled throws for nonexistent hook`() {
        assertThrows(IllegalArgumentException::class.java) {
            hookService.setHookEnabled("nonexistent", true)
        }
    }

    fun `test setHookEnabled throws when hooks dir does not exist`() {
        hooksDir.deleteRecursively()
        assertThrows(IllegalArgumentException::class.java) {
            hookService.setHookEnabled("any-hook", true)
        }
    }

    // ── getExecutionLog ─────────────────────────────────────────────────

    fun `test getExecutionLog returns empty initially`() {
        val log = hookService.getExecutionLog()
        assertTrue(log.isEmpty())
    }

    fun `test getExecutionLog records successful hook execution`() {
        val hook = HookConfig("test-hook", "Test Hook", HookTrigger.FILE_SAVE, true, "test.json")

        hookService.executeHook(hook, "/path/to/file.kt")

        val log = hookService.getExecutionLog()
        assertEquals(1, log.size)
        assertTrue(log[0].success)
        assertEquals("test-hook", log[0].hookId)
        assertEquals("/path/to/file.kt", log[0].triggerFile)
        assertTrue(log[0].message.contains("Test Hook"))
    }

    fun `test getExecutionLog accumulates multiple records`() {
        val hook1 = HookConfig("hook-1", "Hook One", HookTrigger.FILE_SAVE, true, "h1.json")
        val hook2 = HookConfig("hook-2", "Hook Two", HookTrigger.FILE_CREATE, true, "h2.json")

        hookService.executeHook(hook1, "/file1.kt")
        hookService.executeHook(hook2, "/file2.kt")

        val log = hookService.getExecutionLog()
        assertEquals(2, log.size)
        assertEquals("hook-1", log[0].hookId)
        assertEquals("hook-2", log[1].hookId)
    }

    fun `test getExecutionLog returns defensive copy`() {
        val hook = HookConfig("test-hook", "Test", HookTrigger.FILE_SAVE, true, "t.json")
        hookService.executeHook(hook, "/file.kt")

        val log1 = hookService.getExecutionLog()
        val log2 = hookService.getExecutionLog()
        assertEquals(log1.size, log2.size)
        // Modifying the returned list should not affect internal state
        assertFalse(log1 === log2)
    }

    // ── registerEventListeners / unregisterEventListeners ───────────────

    fun `test registerEventListeners does not throw when no hooks exist`() {
        hookService.registerEventListeners()
        // Should complete without error
    }

    fun `test registerEventListeners does not throw when all hooks disabled`() {
        writeHookConfig("disabled-hook", HookConfig("disabled-hook", "Disabled", HookTrigger.FILE_SAVE, false, "d.json"))
        hookService.registerEventListeners()
        // Should complete without error — no listeners registered for disabled hooks
    }

    fun `test unregisterEventListeners can be called multiple times safely`() {
        hookService.unregisterEventListeners()
        hookService.unregisterEventListeners()
        // Should not throw
    }

    fun `test registerEventListeners can be called after unregister`() {
        writeHookConfig("hook", HookConfig("hook", "Hook", HookTrigger.FILE_SAVE, true, "hook.json"))
        hookService.registerEventListeners()
        hookService.unregisterEventListeners()
        hookService.registerEventListeners()
        // Should not throw
    }

    // ── executeHook with null triggerFile ────────────────────────────────

    fun `test executeHook records null triggerFile`() {
        val hook = HookConfig("null-file-hook", "Null File", HookTrigger.PRE_COMMIT, true, "nf.json")

        hookService.executeHook(hook, null)

        val log = hookService.getExecutionLog()
        assertEquals(1, log.size)
        assertNull(log[0].triggerFile)
        assertTrue(log[0].success)
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun writeHookConfig(fileName: String, config: HookConfig) {
        val content = json.encodeToString(HookConfig.serializer(), config)
        File(hooksDir, "$fileName.json").writeText(content)
    }
}
