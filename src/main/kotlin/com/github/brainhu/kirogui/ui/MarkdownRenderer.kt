package com.github.brainhu.kirogui.ui

import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Converts Markdown content to HTML with code highlighting support.
 * Uses CommonMark parser for Markdown processing.
 *
 * Requirements: 2.3
 */
class MarkdownRenderer {

    private val parser: Parser = Parser.builder().build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder()
        .escapeHtml(true)
        .build()

    /**
     * Converts Markdown text to HTML.
     * Code blocks are wrapped with language class for syntax highlighting.
     *
     * @param markdown The Markdown content to convert
     * @return HTML string with proper formatting and code block classes
     */
    fun renderToHtml(markdown: String): String {
        if (markdown.isBlank()) {
            return ""
        }

        try {
            val document = parser.parse(markdown)
            return renderer.render(document)
        } catch (e: Exception) {
            // Fallback to plain text with HTML escaping on parse error
            return "<p>${escapeHtml(markdown)}</p>"
        }
    }

    /**
     * Escapes HTML special characters to prevent XSS.
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * Extracts code blocks from Markdown for special processing.
     * Returns list of (language, code) pairs.
     */
    fun extractCodeBlocks(markdown: String): List<Pair<String, String>> {
        val document = parser.parse(markdown)
        val codeBlocks = mutableListOf<Pair<String, String>>()

        val visitor = object : AbstractVisitor() {
            override fun visit(fencedCodeBlock: FencedCodeBlock) {
                val language = fencedCodeBlock.info ?: "text"
                val code = fencedCodeBlock.literal
                codeBlocks.add(language to code)
            }

            override fun visit(indentedCodeBlock: IndentedCodeBlock) {
                val code = indentedCodeBlock.literal
                codeBlocks.add("text" to code)
            }
        }

        document.accept(visitor)
        return codeBlocks
    }
}
