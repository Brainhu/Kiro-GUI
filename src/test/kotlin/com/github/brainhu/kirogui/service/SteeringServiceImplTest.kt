package com.github.brainhu.kirogui.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Unit tests for [SteeringServiceImpl] covering rule file listing, reading,
 * writing, creation, caching, and VFS file change watching.
 *
 * Requirements: 6.1, 6.2, 6.4
 */
class SteeringServiceImplTest : BasePlatformTestCase() {

    private lateinit var steeringService: SteeringServiceImpl
    private lateinit var steeringDir: File

    override fun setUp() {
        super.setUp()
        steeringService = SteeringServiceImpl(project)
        steeringDir = File(project.basePath!!, ".kiro/steering")
        if (steeringDir.exists()) {
            steeringDir.deleteRecursively()
        }
        steeringDir.mkdirs()
    }

    override fun tearDown() {
        try {
            steeringService.stopWatching()
        } finally {
            super.tearDown()
        }
    }

    // ── listRuleFiles ───────────────────────────────────────────────────

    fun `test listRuleFiles returns empty when no files exist`() {
        val files = steeringService.listRuleFiles()
        assertTrue(files.isEmpty())
    }

    fun `test listRuleFiles returns empty when steering dir does not exist`() {
        steeringDir.deleteRecursively()
        val files = steeringService.listRuleFiles()
        assertTrue(files.isEmpty())
    }

    fun `test listRuleFiles returns markdown files sorted by name`() {
        File(steeringDir, "tech.md").writeText("# Tech")
        File(steeringDir, "product.md").writeText("# Product")
        File(steeringDir, "style.md").writeText("# Style")

        val files = steeringService.listRuleFiles()
        assertEquals(3, files.size)
        assertEquals("product.md", files[0].name)
        assertEquals("style.md", files[1].name)
        assertEquals("tech.md", files[2].name)
    }

    fun `test listRuleFiles ignores non-markdown files`() {
        File(steeringDir, "rules.md").writeText("# Rules")
        File(steeringDir, "notes.txt").writeText("Some notes")
        File(steeringDir, "config.json").writeText("{}")

        val files = steeringService.listRuleFiles()
        assertEquals(1, files.size)
        assertEquals("rules.md", files[0].name)
    }

    fun `test listRuleFiles populates path and lastModified`() {
        val ruleFile = File(steeringDir, "product.md")
        ruleFile.writeText("# Product Context")

        val files = steeringService.listRuleFiles()
        assertEquals(1, files.size)
        assertEquals(ruleFile.absolutePath, files[0].path)
        assertNotNull(files[0].lastModified)
    }

    fun `test listRuleFiles uses cache on second call`() {
        File(steeringDir, "cached.md").writeText("# Cached")

        val first = steeringService.listRuleFiles()
        assertEquals(1, first.size)

        // Add another file on disk but don't invalidate cache
        File(steeringDir, "new.md").writeText("# New")

        val second = steeringService.listRuleFiles()
        // Should still return cached result (1 file)
        assertEquals(1, second.size)
    }

    fun `test listRuleFiles reloads after cache invalidation`() {
        File(steeringDir, "initial.md").writeText("# Initial")
        steeringService.listRuleFiles() // populate cache

        File(steeringDir, "added.md").writeText("# Added")
        steeringService.invalidateCache()

        val files = steeringService.listRuleFiles()
        assertEquals(2, files.size)
    }

    // ── getRuleContent ──────────────────────────────────────────────────

    fun `test getRuleContent returns file content`() {
        val content = "# Product Context\n\nThis is a product rule."
        File(steeringDir, "product.md").writeText(content)

        val result = steeringService.getRuleContent("product.md")
        assertEquals(content, result)
    }

    fun `test getRuleContent throws for nonexistent file`() {
        assertThrows(IllegalArgumentException::class.java) {
            steeringService.getRuleContent("nonexistent.md")
        }
    }

    fun `test getRuleContent reads empty file`() {
        File(steeringDir, "empty.md").writeText("")

        val result = steeringService.getRuleContent("empty.md")
        assertEquals("", result)
    }

    fun `test getRuleContent reads unicode content`() {
        val content = "# 产品上下文\n\n本项目是一个 JetBrains IDE 插件。"
        File(steeringDir, "product.md").writeText(content)

        val result = steeringService.getRuleContent("product.md")
        assertEquals(content, result)
    }

    // ── saveRuleContent ─────────────────────────────────────────────────

    fun `test saveRuleContent writes content to file`() {
        val content = "# Tech Stack\n\n- Kotlin\n- IntelliJ Platform SDK"
        steeringService.saveRuleContent("tech.md", content)

        val onDisk = File(steeringDir, "tech.md").readText()
        assertEquals(content, onDisk)
    }

    fun `test saveRuleContent overwrites existing file`() {
        File(steeringDir, "tech.md").writeText("old content")

        val newContent = "# Updated Tech Stack"
        steeringService.saveRuleContent("tech.md", newContent)

        val onDisk = File(steeringDir, "tech.md").readText()
        assertEquals(newContent, onDisk)
    }

    fun `test saveRuleContent invalidates cache`() {
        File(steeringDir, "rule.md").writeText("# Original")
        steeringService.listRuleFiles() // populate cache

        steeringService.saveRuleContent("rule.md", "# Updated")

        // Cache should be invalidated, so next listRuleFiles reads from disk
        assertNull(steeringService.cachedRuleFiles)
    }

    fun `test saveRuleContent creates steering dir if missing`() {
        steeringDir.deleteRecursively()

        steeringService.saveRuleContent("new-rule.md", "# New Rule")

        assertTrue(steeringDir.exists())
        assertEquals("# New Rule", File(steeringDir, "new-rule.md").readText())
    }

    fun `test saveRuleContent round-trip preserves content`() {
        val content = "# Complex Rule\n\n```kotlin\nfun main() {}\n```\n\n- Item 1\n- Item 2"
        steeringService.saveRuleContent("complex.md", content)

        val loaded = steeringService.getRuleContent("complex.md")
        assertEquals(content, loaded)
    }

    // ── createRuleFile ──────────────────────────────────────────────────

    fun `test createRuleFile creates empty file and returns metadata`() {
        val result = steeringService.createRuleFile("new-rule.md")

        assertEquals("new-rule.md", result.name)
        assertTrue(File(steeringDir, "new-rule.md").exists())
        assertEquals("", File(steeringDir, "new-rule.md").readText())
        assertNotNull(result.lastModified)
    }

    fun `test createRuleFile throws if file already exists`() {
        File(steeringDir, "existing.md").writeText("# Existing")

        assertThrows(IllegalArgumentException::class.java) {
            steeringService.createRuleFile("existing.md")
        }
    }

    fun `test createRuleFile invalidates cache`() {
        steeringService.listRuleFiles() // populate cache

        steeringService.createRuleFile("brand-new.md")

        assertNull(steeringService.cachedRuleFiles)
    }

    fun `test createRuleFile creates steering dir if missing`() {
        steeringDir.deleteRecursively()

        steeringService.createRuleFile("auto-dir.md")

        assertTrue(steeringDir.exists())
        assertTrue(File(steeringDir, "auto-dir.md").exists())
    }

    // ── watchForChanges / stopWatching ──────────────────────────────────

    fun `test watchForChanges does not throw`() {
        steeringService.watchForChanges()
        // Should complete without error
    }

    fun `test stopWatching can be called multiple times safely`() {
        steeringService.stopWatching()
        steeringService.stopWatching()
        // Should not throw
    }

    fun `test watchForChanges can be called after stopWatching`() {
        steeringService.watchForChanges()
        steeringService.stopWatching()
        steeringService.watchForChanges()
        // Should not throw
    }

    fun `test watchForChanges replaces previous listener`() {
        steeringService.watchForChanges()
        steeringService.watchForChanges()
        // Should not throw — previous connection is disconnected first
    }
}
