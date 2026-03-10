package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.Position
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Unit tests for [ContextServiceImpl] verifying context collection
 * from Editor instances using IntelliJ Platform test fixtures.
 *
 * Requirements: 7.2, 2.7
 */
class ContextServiceImplTest : BasePlatformTestCase() {

    private lateinit var contextService: ContextServiceImpl

    override fun setUp() {
        super.setUp()
        contextService = ContextServiceImpl(project)
    }

    // ── File path ───────────────────────────────────────────────────────

    fun `test getCurrentFilePath returns path for open file`() {
        myFixture.configureByText("Example.kt", "fun main() {}")
        val editor = myFixture.editor

        val path = contextService.getCurrentFilePath(editor)

        assertNotNull(path)
        assertTrue(path!!.endsWith("Example.kt"))
    }

    // ── Selected text ───────────────────────────────────────────────────

    fun `test getSelectedText returns null when nothing selected`() {
        myFixture.configureByText("NoSelect.kt", "val x = 1")
        val editor = myFixture.editor

        assertNull(contextService.getSelectedText(editor))
    }

    fun `test getSelectedText returns selected text`() {
        myFixture.configureByText("Select.kt", "val greeting = \"hello world\"")
        val editor = myFixture.editor

        // Select "hello world" (inside the quotes)
        val text = editor.document.text
        val start = text.indexOf("hello world")
        val end = start + "hello world".length
        editor.selectionModel.setSelection(start, end)

        val selected = contextService.getSelectedText(editor)
        assertEquals("hello world", selected)
    }

    // ── Cursor position ─────────────────────────────────────────────────

    fun `test getCursorPosition returns position at start of file`() {
        myFixture.configureByText("Cursor.kt", "fun main() {}")
        val editor = myFixture.editor

        // Caret is at position 0 by default
        editor.caretModel.moveToOffset(0)
        val pos = contextService.getCursorPosition(editor)

        assertNotNull(pos)
        assertEquals(Position(line = 0, character = 0), pos)
    }

    fun `test getCursorPosition returns correct line and column`() {
        myFixture.configureByText(
            "MultiLine.kt",
            "line one\nline two\nline three"
        )
        val editor = myFixture.editor

        // Move to start of "line two" → line 1, column 0
        val offset = editor.document.text.indexOf("line two")
        editor.caretModel.moveToOffset(offset)
        val pos = contextService.getCursorPosition(editor)

        assertNotNull(pos)
        assertEquals(1, pos!!.line)
        assertEquals(0, pos.character)
    }

    // ── File language ───────────────────────────────────────────────────

    fun `test getFileLanguage returns non-null language id`() {
        myFixture.configureByText("Lang.java", "class Lang {}")
        val editor = myFixture.editor

        val lang = contextService.getFileLanguage(editor)

        assertNotNull(lang)
        assertTrue(lang!!.isNotEmpty())
    }

    fun `test getFileLanguage returns non-empty for java file`() {
        myFixture.configureByText("Lang.java", "class Lang {}")
        val editor = myFixture.editor

        val lang = contextService.getFileLanguage(editor)

        assertNotNull(lang)
        assertTrue(lang!!.isNotEmpty())
    }

    fun `test getFileLanguage returns language for plain text`() {
        myFixture.configureByText("readme.txt", "Hello")
        val editor = myFixture.editor

        val lang = contextService.getFileLanguage(editor)

        assertNotNull(lang)
        assertTrue(lang!!.isNotEmpty())
    }

    // ── collectContext (integration of all fields) ──────────────────────

    fun `test collectContext populates all fields`() {
        myFixture.configureByText("Full.java", "class Full {\n    void hello() {}\n}")
        val editor = myFixture.editor

        // Select "hello"
        val text = editor.document.text
        val start = text.indexOf("hello")
        val end = start + "hello".length
        editor.selectionModel.setSelection(start, end)
        editor.caretModel.moveToOffset(start)

        val ctx = contextService.collectContext(editor)

        assertNotNull(ctx.filePath)
        assertTrue(ctx.filePath!!.endsWith("Full.java"))
        assertEquals("hello", ctx.selectedText)
        assertNotNull(ctx.cursorPosition)
        assertEquals(1, ctx.cursorPosition!!.line)
        assertNotNull(ctx.language)
    }

    fun `test collectContext with no selection has null selectedText`() {
        myFixture.configureByText("NoSel.kt", "val x = 42")
        val editor = myFixture.editor

        val ctx = contextService.collectContext(editor)

        assertNotNull(ctx.filePath)
        assertNull(ctx.selectedText)
        assertNotNull(ctx.cursorPosition)
        assertNotNull(ctx.language)
    }
}
