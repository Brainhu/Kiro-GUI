package com.github.brainhu.kirogui.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Unit tests for MarkdownPreviewPanel with theme adaptation.
 * 
 * Requirements: 3.4, 9.1, 9.2, 9.3, 9.4
 */
class MarkdownPreviewPanelTest : BasePlatformTestCase() {

    fun testMarkdownPreviewPanelCreation() {
        // Test that MarkdownPreviewPanel can be instantiated
        val panel = MarkdownPreviewPanel(project)
        assertNotNull(panel)
        panel.dispose()
    }

    fun testSetMarkdownContent() {
        // Test that markdown content can be set
        val panel = MarkdownPreviewPanel(project)
        
        val markdown = """
            # Test Document
            
            This is a test document with **bold** and *italic* text.
            
            ```kotlin
            fun main() {
                println("Hello, World!")
            }
            ```
        """.trimIndent()
        
        // Should not throw exception
        panel.setMarkdownContent(markdown)
        
        panel.dispose()
    }

    fun testSetEmptyMarkdownContent() {
        // Test that empty content can be set
        val panel = MarkdownPreviewPanel(project)
        
        // Should not throw exception
        panel.setMarkdownContent("")
        
        panel.dispose()
    }

    fun testThemeAdapterIntegration() {
        // Test that MarkdownPreviewPanel integrates with ThemeAdapter
        val panel = MarkdownPreviewPanel(project)
        val themeAdapter = ThemeAdapter.getInstance()
        
        // Verify theme adapter is available
        assertNotNull(themeAdapter)
        
        // Verify CSS variables can be generated
        val cssVars = themeAdapter.generateCssVariables()
        assertTrue(cssVars.isNotEmpty())
        
        // Verify CSS variables contain required keys
        assertTrue(cssVars.containsKey("--bg-color"))
        assertTrue(cssVars.containsKey("--text-color"))
        assertTrue(cssVars.containsKey("--code-bg"))
        
        panel.dispose()
    }

    fun testMultipleContentUpdates() {
        // Test that content can be updated multiple times
        val panel = MarkdownPreviewPanel(project)
        
        panel.setMarkdownContent("# First Content")
        panel.setMarkdownContent("# Second Content")
        panel.setMarkdownContent("# Third Content")
        
        // Should not throw exception
        panel.dispose()
    }

    fun testMarkdownWithCodeBlocks() {
        // Test rendering markdown with code blocks
        val panel = MarkdownPreviewPanel(project)
        
        val markdown = """
            ## Code Examples
            
            Kotlin example:
            ```kotlin
            data class User(val name: String)
            ```
            
            Java example:
            ```java
            public class User {
                private String name;
            }
            ```
        """.trimIndent()
        
        panel.setMarkdownContent(markdown)
        
        panel.dispose()
    }

    fun testMarkdownWithTables() {
        // Test rendering markdown with tables
        val panel = MarkdownPreviewPanel(project)
        
        val markdown = """
            | Column 1 | Column 2 |
            |----------|----------|
            | Value 1  | Value 2  |
            | Value 3  | Value 4  |
        """.trimIndent()
        
        panel.setMarkdownContent(markdown)
        
        panel.dispose()
    }
}
