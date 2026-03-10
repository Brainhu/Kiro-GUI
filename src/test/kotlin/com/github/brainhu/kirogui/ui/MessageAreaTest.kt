package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.ChatMessage
import com.github.brainhu.kirogui.model.MessageRole
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.time.Instant

/**
 * Unit tests for MessageArea JCEF-based message rendering.
 * 
 * Requirements: 2.3, 2.4, 2.5
 */
class MessageAreaTest : BasePlatformTestCase() {

    fun testMessageAreaCreation() {
        // Test that MessageArea can be instantiated
        val messageArea = MessageArea(project)
        assertNotNull(messageArea)
        messageArea.dispose()
    }

    fun testAddMessage() {
        // Test that messages can be added without errors
        val messageArea = MessageArea(project)
        
        val message = ChatMessage(
            id = "test-msg-1",
            role = MessageRole.USER,
            content = "Hello, Kiro!",
            timestamp = Instant.now(),
            context = null
        )
        
        // Should not throw exception
        messageArea.addMessage(message, isStreaming = false)
        
        messageArea.dispose()
    }

    fun testStreamingMessage() {
        // Test that streaming messages can be updated
        val messageArea = MessageArea(project)
        
        val message = ChatMessage(
            id = "test-msg-2",
            role = MessageRole.ASSISTANT,
            content = "Generating response...",
            timestamp = Instant.now(),
            context = null
        )
        
        // Add as streaming
        messageArea.addMessage(message, isStreaming = true)
        
        // Update with more content
        val updatedMessage = message.copy(content = "Generating response... complete!")
        messageArea.addMessage(updatedMessage, isStreaming = false)
        
        messageArea.dispose()
    }

    fun testClearMessages() {
        // Test that messages can be cleared
        val messageArea = MessageArea(project)
        
        val message = ChatMessage(
            id = "test-msg-3",
            role = MessageRole.USER,
            content = "Test message",
            timestamp = Instant.now(),
            context = null
        )
        
        messageArea.addMessage(message, isStreaming = false)
        messageArea.clearMessages()
        
        messageArea.dispose()
    }

    fun testThemeUpdate() {
        // Test that theme can be updated without errors
        val messageArea = MessageArea(project)
        
        // Should not throw exception
        messageArea.updateTheme()
        
        messageArea.dispose()
    }
    
    fun testThemeAdapterIntegration() {
        // Test that MessageArea integrates with ThemeAdapter
        val messageArea = MessageArea(project)
        val themeAdapter = ThemeAdapter.getInstance()
        
        // Verify theme adapter is available
        assertNotNull(themeAdapter)
        
        // Verify CSS variables can be generated
        val cssVars = themeAdapter.generateCssVariables()
        assertTrue(cssVars.isNotEmpty())
        
        messageArea.dispose()
    }
}
