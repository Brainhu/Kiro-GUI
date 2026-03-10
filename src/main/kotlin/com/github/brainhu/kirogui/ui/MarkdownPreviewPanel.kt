package com.github.brainhu.kirogui.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout

/**
 * JCEF-based Markdown preview panel for displaying spec documents.
 *
 * Requirements: 3.4, 9.1, 9.2, 9.3, 9.4
 */
class MarkdownPreviewPanel(private val project: Project) : JBPanel<MarkdownPreviewPanel>(BorderLayout()) {
    private val browser: JBCefBrowser = JBCefBrowser()
    private val markdownRenderer = MarkdownRenderer()
    private val themeAdapter = ThemeAdapter.getInstance()
    private var currentMarkdown: String = ""
    
    init {
        add(browser.component, BorderLayout.CENTER)
        
        // Register theme change listener
        themeAdapter.addThemeChangeListener {
            // Reload content with new theme
            setMarkdownContent(currentMarkdown)
        }
        
        // Set initial empty content
        setMarkdownContent("")
    }
    
    fun setMarkdownContent(markdown: String) {
        currentMarkdown = markdown
        val html = markdownRenderer.renderToHtml(markdown)
        val styledHtml = wrapWithStyles(html)
        browser.loadHTML(styledHtml)
    }
    
    private fun wrapWithStyles(html: String): String {
        val cssVars = themeAdapter.generateCssVariables()
        val isDark = themeAdapter.isDarkTheme()
        
        // Generate CSS variables section
        val cssVarsSection = cssVars.entries.joinToString("\n") { (key, value) ->
            "        $key: $value;"
        }
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    :root {
$cssVarsSection
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                        color: var(--text-color);
                        background-color: var(--bg-color);
                    }
                    h1, h2, h3, h4, h5, h6 {
                        margin-top: 24px;
                        margin-bottom: 16px;
                        font-weight: 600;
                        line-height: 1.25;
                        color: var(--text-color);
                    }
                    h1 { 
                        font-size: 2em; 
                        border-bottom: 1px solid var(--border-color); 
                        padding-bottom: 0.3em; 
                    }
                    h2 { 
                        font-size: 1.5em; 
                        border-bottom: 1px solid var(--border-color); 
                        padding-bottom: 0.3em; 
                    }
                    h3 { font-size: 1.25em; }
                    code {
                        background-color: var(--code-bg);
                        color: var(--code-fg);
                        border-radius: 3px;
                        font-size: 85%;
                        margin: 0;
                        padding: 0.2em 0.4em;
                        font-family: 'JetBrains Mono', 'Consolas', 'Monaco', monospace;
                    }
                    pre {
                        background-color: var(--code-bg);
                        border: 1px solid var(--code-border);
                        border-radius: 3px;
                        font-size: 85%;
                        line-height: 1.45;
                        overflow: auto;
                        padding: 16px;
                    }
                    pre code {
                        background-color: transparent;
                        border: 0;
                        display: inline;
                        line-height: inherit;
                        margin: 0;
                        overflow: visible;
                        padding: 0;
                        word-wrap: normal;
                    }
                    blockquote {
                        border-left: 4px solid var(--border-color);
                        color: var(--syntax-comment);
                        padding: 0 1em;
                        margin: 0;
                    }
                    table {
                        border-collapse: collapse;
                        border-spacing: 0;
                        width: 100%;
                        margin-bottom: 16px;
                    }
                    table th, table td {
                        border: 1px solid var(--border-color);
                        padding: 6px 13px;
                    }
                    table th {
                        background-color: ${if (isDark) "rgba(255, 255, 255, 0.05)" else "rgba(0, 0, 0, 0.03)"};
                        font-weight: 600;
                    }
                    ul, ol {
                        padding-left: 2em;
                        margin-bottom: 16px;
                    }
                    li {
                        margin-bottom: 4px;
                    }
                    p {
                        margin-bottom: 16px;
                    }
                    a {
                        color: var(--link-color);
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                </style>
            </head>
            <body>
                $html
            </body>
            </html>
        """.trimIndent()
    }
    
    fun dispose() {
        browser.dispose()
    }
}
