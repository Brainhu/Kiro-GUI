package com.github.brainhu.kirogui.actions

import com.github.brainhu.kirogui.model.MessageContext
import com.github.brainhu.kirogui.model.Position
import com.github.brainhu.kirogui.service.ChatService
import com.github.brainhu.kirogui.service.ContextService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

/**
 * Unit tests for editor context actions.
 * Tests that actions correctly collect context and send appropriate prompts to ChatService.
 * 
 * Requirements: 7.1, 7.2, 7.4
 */
class EditorContextActionsTest : BasePlatformTestCase() {

    private lateinit var mockContextService: ContextService
    private lateinit var mockChatService: ChatService
    private lateinit var mockEditor: Editor
    private lateinit var mockSelectionModel: SelectionModel

    override fun setUp() {
        super.setUp()
        
        // Create mocks
        mockContextService = mockk()
        mockChatService = mockk()
        mockEditor = mockk()
        mockSelectionModel = mockk()
        
        // Setup editor mock
        every { mockEditor.selectionModel } returns mockSelectionModel
    }

    @Test
    fun testExplainCodeAction_collectsContextAndSendsMessage() {
        // Arrange
        val selectedText = "fun hello() { println(\"Hello\") }"
        val context = MessageContext(
            filePath = "test.kt",
            selectedText = selectedText,
            cursorPosition = Position(1, 0),
            language = "kotlin"
        )
        
        every { mockSelectionModel.hasSelection() } returns true
        every { mockContextService.collectContext(mockEditor) } returns context
        every { mockChatService.getAllSessions() } returns emptyList()
        every { mockChatService.createSession() } returns mockk {
            every { id } returns "session-1"
        }
        coEvery { mockChatService.sendMessage(any(), any(), any()) } returns flowOf()
        
        // Create action event
        val event = createMockActionEvent(mockEditor)
        
        // Act
        val action = ExplainCodeAction()
        action.actionPerformed(event)
        
        // Assert
        coVerify {
            mockChatService.sendMessage(
                "session-1",
                match { it.contains("explain") && it.contains(selectedText) },
                context
            )
        }
    }

    @Test
    fun testRefactorSuggestionAction_collectsContextAndSendsMessage() {
        // Arrange
        val selectedText = "var x = 1; var y = 2;"
        val context = MessageContext(
            filePath = "test.kt",
            selectedText = selectedText,
            cursorPosition = Position(1, 0),
            language = "kotlin"
        )
        
        every { mockSelectionModel.hasSelection() } returns true
        every { mockContextService.collectContext(mockEditor) } returns context
        every { mockChatService.getAllSessions() } returns emptyList()
        every { mockChatService.createSession() } returns mockk {
            every { id } returns "session-1"
        }
        coEvery { mockChatService.sendMessage(any(), any(), any()) } returns flowOf()
        
        // Create action event
        val event = createMockActionEvent(mockEditor)
        
        // Act
        val action = RefactorSuggestionAction()
        action.actionPerformed(event)
        
        // Assert
        coVerify {
            mockChatService.sendMessage(
                "session-1",
                match { it.contains("refactor") && it.contains(selectedText) },
                context
            )
        }
    }

    @Test
    fun testGenerateTestAction_collectsContextAndSendsMessage() {
        // Arrange
        val selectedText = "fun add(a: Int, b: Int) = a + b"
        val context = MessageContext(
            filePath = "test.kt",
            selectedText = selectedText,
            cursorPosition = Position(1, 0),
            language = "kotlin"
        )
        
        every { mockSelectionModel.hasSelection() } returns true
        every { mockContextService.collectContext(mockEditor) } returns context
        every { mockChatService.getAllSessions() } returns emptyList()
        every { mockChatService.createSession() } returns mockk {
            every { id } returns "session-1"
        }
        coEvery { mockChatService.sendMessage(any(), any(), any()) } returns flowOf()
        
        // Create action event
        val event = createMockActionEvent(mockEditor)
        
        // Act
        val action = GenerateTestAction()
        action.actionPerformed(event)
        
        // Assert
        coVerify {
            mockChatService.sendMessage(
                "session-1",
                match { it.contains("test") && it.contains(selectedText) },
                context
            )
        }
    }

    @Test
    fun testAddCommentAction_collectsContextAndSendsMessage() {
        // Arrange
        val selectedText = "fun complex() { /* complex logic */ }"
        val context = MessageContext(
            filePath = "test.kt",
            selectedText = selectedText,
            cursorPosition = Position(1, 0),
            language = "kotlin"
        )
        
        every { mockSelectionModel.hasSelection() } returns true
        every { mockContextService.collectContext(mockEditor) } returns context
        every { mockChatService.getAllSessions() } returns emptyList()
        every { mockChatService.createSession() } returns mockk {
            every { id } returns "session-1"
        }
        coEvery { mockChatService.sendMessage(any(), any(), any()) } returns flowOf()
        
        // Create action event
        val event = createMockActionEvent(mockEditor)
        
        // Act
        val action = AddCommentAction()
        action.actionPerformed(event)
        
        // Assert
        coVerify {
            mockChatService.sendMessage(
                "session-1",
                match { it.contains("comment") && it.contains(selectedText) },
                context
            )
        }
    }

    @Test
    fun testActions_disabledWhenNoSelection() {
        // Arrange
        every { mockSelectionModel.hasSelection() } returns false
        val event = createMockActionEvent(mockEditor)
        
        // Act & Assert
        val actions = listOf(
            ExplainCodeAction(),
            RefactorSuggestionAction(),
            GenerateTestAction(),
            AddCommentAction(),
            SendToChatAction()
        )
        
        actions.forEach { action ->
            action.update(event)
            assertFalse("Action ${action.javaClass.simpleName} should be disabled when no selection", 
                event.presentation.isEnabled)
        }
    }

    private fun createMockActionEvent(editor: Editor): AnActionEvent {
        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns project
        every { event.getData(CommonDataKeys.EDITOR) } returns editor
        every { event.presentation } returns mockk(relaxed = true) {
            var enabled = true
            every { isEnabled } answers { enabled }
            every { isEnabled = any() } answers { enabled = firstArg() }
        }
        return event
    }
}
