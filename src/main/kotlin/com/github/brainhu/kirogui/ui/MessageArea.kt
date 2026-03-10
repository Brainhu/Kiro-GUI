package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.ChatMessage
import com.github.brainhu.kirogui.model.MessageRole
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JPanel

/**
 * JCEF-based message rendering area for the Chat Panel.
 * Renders messages with Markdown support, code highlighting, and interactive elements.
 *
 * Requirements: 2.3, 2.4, 2.5
 */
class MessageArea(private val project: Project) : JPanel(BorderLayout()) {

    private val logger = Logger.getInstance(MessageArea::class.java)
    private val browser: JBCefBrowser = JBCefBrowser()
    private val markdownRenderer = MarkdownRenderer()
    private val themeAdapter = ThemeAdapter.getInstance()
    
    private var isPageLoaded = false
    private val pendingMessages = mutableListOf<PendingMessage>()

    // JavaScript bridge queries for communication from JS to Kotlin
    private val copyToClipboardQuery: JBCefJSQuery
    private val applyToEditorQuery: JBCefJSQuery
    private val openFileQuery: JBCefJSQuery

    init {
        // Initialize JS queries for bidirectional communication
        copyToClipboardQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
        copyToClipboardQuery.addHandler { code ->
            handleCopyToClipboard(code)
            null
        }

        applyToEditorQuery = JBCefJSQuery.create(browser)
        applyToEditorQuery.addHandler { params ->
            handleApplyToEditor(params)
            null
        }

        openFileQuery = JBCefJSQuery.create(browser)
        openFileQuery.addHandler { params ->
            handleOpenFile(params)
            null
        }

        // Add browser component to panel
        add(browser.component, BorderLayout.CENTER)

        // Register theme change listener
        themeAdapter.addThemeChangeListener {
            if (isPageLoaded) {
                updateTheme()
            }
        }

        // Load HTML template
        loadHtmlTemplate()
    }

    private fun loadHtmlTemplate() {
        val htmlContent = loadResourceAsString("/html/chat-template.html")
        val cssContent = loadResourceAsString("/html/chat-styles.css")
        val jsContent = loadResourceAsString("/html/chat-script.js")

        // Get highlight.js theme from ThemeAdapter
        val highlightTheme = themeAdapter.getHighlightJsTheme()

        // Inject CSS and JS into HTML with dynamic highlight.js theme
        val fullHtml = htmlContent
            .replace("github-dark.min.css", highlightTheme.substringAfterLast("/"))
            .replace("</head>", "<style>$cssContent</style></head>")
            .replace("</body>", "<script>$jsContent</script>${createBridgeScript()}</body>")

        // Add load handler to track when page is ready
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    isPageLoaded = true
                    updateTheme()
                    
                    // Process pending messages
                    ApplicationManager.getApplication().invokeLater {
                        pendingMessages.forEach { pending ->
                            addMessageInternal(pending.message, pending.isStreaming)
                        }
                        pendingMessages.clear()
                    }
                }
            }
        }, browser.cefBrowser)

        browser.loadHTML(fullHtml)
    }

    private fun createBridgeScript(): String {
        return """
            <script>
                // Create Java bridge object
                window.kiroJavaBridge = {
                    copyToClipboard: function(code) {
                        ${copyToClipboardQuery.inject("code")}
                    },
                    applyToEditor: function(code, language) {
                        ${applyToEditorQuery.inject("code + '|||' + language")}
                    },
                    openFile: function(filePath, line) {
                        ${openFileQuery.inject("filePath + '|||' + line")}
                    }
                };
            </script>
        """.trimIndent()
    }

    /**
     * Adds a message to the display.
     * If the message is streaming, it can be updated by calling this method again with the same message ID.
     */
    fun addMessage(message: ChatMessage, isStreaming: Boolean = false) {
        if (!isPageLoaded) {
            // Queue message until page is loaded
            pendingMessages.add(PendingMessage(message, isStreaming))
            return
        }

        addMessageInternal(message, isStreaming)
    }

    private fun addMessageInternal(message: ChatMessage, isStreaming: Boolean) {
        val htmlContent = markdownRenderer.renderToHtml(message.content)
        val escapedContent = escapeForJs(htmlContent)
        val role = message.role.name

        val js = """
            addMessage('${message.id}', '$role', '$escapedContent', $isStreaming);
        """.trimIndent()

        executeJs(js)
    }

    /**
     * Clears all messages from the display.
     */
    fun clearMessages() {
        executeJs("clearMessages();")
    }

    /**
     * Updates the theme colors based on current IDE theme.
     */
    fun updateTheme() {
        val cssVars = themeAdapter.generateCssVariables()
        
        val cssVarsJson = cssVars.entries.joinToString(",") { (k, v) -> 
            "'$k': '$v'" 
        }
        
        executeJs("updateTheme({$cssVarsJson});")
    }

    private fun executeJs(js: String) {
        ApplicationManager.getApplication().invokeLater {
            browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
        }
    }

    private fun handleCopyToClipboard(code: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val selection = StringSelection(code)
                CopyPasteManager.getInstance().setContents(selection)
                logger.info("Code copied to clipboard")
            } catch (e: Exception) {
                logger.error("Failed to copy code to clipboard", e)
            }
        }
    }

    private fun handleApplyToEditor(params: String) {
        val parts = params.split("|||")
        if (parts.size != 2) return

        val code = parts[0]
        val language = parts[1]

        ApplicationManager.getApplication().invokeLater {
            try {
                val editor = FileEditorManager.getInstance(project).selectedTextEditor
                if (editor != null) {
                    val document = editor.document
                    val caretModel = editor.caretModel
                    val offset = caretModel.offset

                    ApplicationManager.getApplication().runWriteAction {
                        document.insertString(offset, code)
                        caretModel.moveToOffset(offset + code.length)
                    }
                    logger.info("Code applied to editor")
                } else {
                    logger.warn("No active editor to apply code")
                }
            } catch (e: Exception) {
                logger.error("Failed to apply code to editor", e)
            }
        }
    }

    private fun handleOpenFile(params: String) {
        val parts = params.split("|||")
        if (parts.isEmpty()) return

        val filePath = parts[0]
        val line = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

        ApplicationManager.getApplication().invokeLater {
            try {
                val projectBasePath = project.basePath ?: return@invokeLater
                val file = File(projectBasePath, filePath)
                
                if (!file.exists()) {
                    logger.warn("File not found: $filePath")
                    return@invokeLater
                }

                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
                if (virtualFile != null) {
                    val descriptor = OpenFileDescriptor(project, virtualFile, line - 1, 0)
                    FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
                    logger.info("Opened file: $filePath at line $line")
                } else {
                    logger.warn("Virtual file not found: $filePath")
                }
            } catch (e: Exception) {
                logger.error("Failed to open file: $filePath", e)
            }
        }
    }

    private fun loadResourceAsString(path: String): String {
        return try {
            this::class.java.getResourceAsStream(path)?.bufferedReader()?.readText() 
                ?: throw IllegalStateException("Resource not found: $path")
        } catch (e: Exception) {
            logger.error("Failed to load resource: $path", e)
            ""
        }
    }

    private fun escapeForJs(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun dispose() {
        copyToClipboardQuery.dispose()
        applyToEditorQuery.dispose()
        openFileQuery.dispose()
        browser.dispose()
    }

    private data class PendingMessage(
        val message: ChatMessage,
        val isStreaming: Boolean
    )
}
