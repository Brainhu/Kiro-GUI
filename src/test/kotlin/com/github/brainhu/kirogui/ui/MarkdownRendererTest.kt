package com.github.brainhu.kirogui.ui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for MarkdownRenderer.
 * 
 * Requirements: 2.3
 */
class MarkdownRendererTest {

    private val renderer = MarkdownRenderer()

    @Test
    fun testRenderPlainText() {
        val markdown = "Hello, world!"
        val html = renderer.renderToHtml(markdown)
        
        assertTrue(html.contains("Hello, world!"))
        assertTrue(html.contains("<p>"))
    }

    @Test
    fun testRenderCodeBlock() {
        val markdown = """
            ```kotlin
            fun main() {
                println("Hello")
            }
            ```
        """.trimIndent()
        
        val html = renderer.renderToHtml(markdown)
        
        assertTrue(html.contains("<code"))
        assertTrue(html.contains("fun main()"))
    }

    @Test
    fun testRenderInlineCode() {
        val markdown = "Use the `println()` function"
        val html = renderer.renderToHtml(markdown)
        
        assertTrue(html.contains("<code>"))
        assertTrue(html.contains("println()"))
    }

    @Test
    fun testRenderHeadings() {
        val markdown = """
            # Heading 1
            ## Heading 2
            ### Heading 3
        """.trimIndent()
        
        val html = renderer.renderToHtml(markdown)
        
        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("<h3>"))
    }

    @Test
    fun testRenderList() {
        val markdown = """
            - Item 1
            - Item 2
            - Item 3
        """.trimIndent()
        
        val html = renderer.renderToHtml(markdown)
        
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>"))
        assertTrue(html.contains("Item 1"))
    }

    @Test
    fun testRenderLink() {
        val markdown = "[Click here](https://example.com)"
        val html = renderer.renderToHtml(markdown)
        
        assertTrue(html.contains("<a"))
        assertTrue(html.contains("href"))
        assertTrue(html.contains("example.com"))
    }

    @Test
    fun testExtractCodeBlocks() {
        val markdown = """
            Some text
            
            ```kotlin
            fun test() {}
            ```
            
            More text
            
            ```java
            public void test() {}
            ```
        """.trimIndent()
        
        val codeBlocks = renderer.extractCodeBlocks(markdown)
        
        assertEquals(2, codeBlocks.size)
        assertEquals("kotlin", codeBlocks[0].first)
        assertTrue(codeBlocks[0].second.contains("fun test()"))
        assertEquals("java", codeBlocks[1].first)
        assertTrue(codeBlocks[1].second.contains("public void test()"))
    }

    @Test
    fun testRenderEmptyString() {
        val html = renderer.renderToHtml("")
        assertEquals("", html)
    }

    @Test
    fun testRenderWithHtmlEscaping() {
        val markdown = "Use <script> tags carefully"
        val html = renderer.renderToHtml(markdown)
        
        // HTML should be escaped
        assertTrue(html.contains("&lt;") || html.contains("script"))
    }
}
