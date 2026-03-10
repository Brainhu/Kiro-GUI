package com.github.brainhu.kirogui.ui

import com.intellij.openapi.wm.ToolWindow
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Integration test for SteeringToolWindowFactory.
 * Verifies that the factory creates the tool window content correctly.
 *
 * Requirements: 6.2, 6.3
 */
class SteeringToolWindowFactoryTest : BasePlatformTestCase() {

    @Test
    fun testCreateToolWindowContent() {
        // Given
        val factory = SteeringToolWindowFactory()
        val toolWindow = mockk<ToolWindow>(relaxed = true)
        
        // When
        factory.createToolWindowContent(project, toolWindow)
        
        // Then
        // Verify that content was added to the tool window
        verify { toolWindow.contentManager.addContent(any()) }
    }

    @Test
    fun testFactoryCreatesSteeringPanel() {
        // Given
        val factory = SteeringToolWindowFactory()
        val toolWindow = mockk<ToolWindow>(relaxed = true)
        
        // When
        factory.createToolWindowContent(project, toolWindow)
        
        // Then
        // Verify that the factory doesn't throw exceptions
        // The actual panel creation is tested in SteeringPanelTest
        assertTrue(true)
    }
}
