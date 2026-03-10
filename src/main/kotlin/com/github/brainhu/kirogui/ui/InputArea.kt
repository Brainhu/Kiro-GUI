package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.MessageContext
import com.github.brainhu.kirogui.service.ChatService
import com.github.brainhu.kirogui.service.ContextService
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel

/**
 * Input area for composing and sending messages.
 * Contains multi-line text area, attach button, mode selector, and send button.
 *
 * Requirements: 2.2, 2.7
 */
class InputArea(
    private val project: Project,
    private val chatService: ChatService,
    private val contextService: ContextService,
    private val contextChipBar: ContextChipBar,
    private val messageArea: MessageArea
) : JBPanel<InputArea>(BorderLayout()) {

    private val textArea = JBTextArea()
    private val attachButton = JButton("📎")
    private val modeSelector = JComboBox(arrayOf("Chat", "Code", "Debug"))
    private val sendButton = JButton("▶")

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        // Configure text area
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.rows = 3
        textArea.toolTipText = "Type your message (Shift+Enter for new line, Enter to send)"

        // Handle Enter key to send (Shift+Enter for new line)
        textArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                    e.consume()
                    sendMessage()
                }
            }
        })

        val scrollPane = JBScrollPane(textArea)

        // Configure buttons
        attachButton.toolTipText = "Attach file or code selection"
        attachButton.addActionListener {
            attachContext()
        }

        modeSelector.toolTipText = "Select interaction mode"

        sendButton.toolTipText = "Send message"
        sendButton.addActionListener {
            sendMessage()
        }

        // Button panel at bottom
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 5)).apply {
            add(attachButton)
            add(modeSelector)
            add(sendButton)
        }

        // Layout
        add(scrollPane, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }

    /**
     * Attach current editor context as a chip.
     */
    private fun attachContext() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            val context = contextService.collectContext(editor)
            val label = buildContextLabel(context)
            contextChipBar.addChip(label, context)
        }
    }

    /**
     * Build a human-readable label for the context chip.
     */
    private fun buildContextLabel(context: MessageContext): String {
        return when {
            context.selectedText != null -> {
                val preview = context.selectedText.take(30)
                "📄 ${context.filePath?.substringAfterLast('/') ?: "Selection"}: $preview..."
            }
            context.filePath != null -> {
                "📄 ${context.filePath.substringAfterLast('/')}"
            }
            else -> "📄 Context"
        }
    }

    /**
     * Send the current message to the chat service.
     */
    private fun sendMessage() {
        val messageText = textArea.text.trim()
        if (messageText.isEmpty()) return

        // Get current session (assume first session for now - will be improved with tab selection)
        val sessions = chatService.getAllSessions()
        if (sessions.isEmpty()) {
            chatService.createSession()
        }
        val session = chatService.getAllSessions().firstOrNull() ?: return

        // Collect context from chips
        val chipData = contextChipBar.getChipData()
        val context = chipData.firstOrNull() as? MessageContext

        // Clear input
        textArea.text = ""
        contextChipBar.clearChips()

        // Send message asynchronously
        scope.launch {
            try {
                val responseFlow = chatService.sendMessage(session.id, messageText, context)
                
                // Display user message immediately
                val userMessage = session.messages.lastOrNull { it.role == com.github.brainhu.kirogui.model.MessageRole.USER }
                if (userMessage != null) {
                    messageArea.addMessage(userMessage, isStreaming = false)
                }
                
                // Collect streaming response chunks
                var assistantMessageId: String? = null
                val contentBuilder = StringBuilder()
                
                responseFlow.collect { chunk ->
                    contentBuilder.append(chunk.content)
                    
                    // Get or create assistant message ID
                    if (assistantMessageId == null) {
                        assistantMessageId = "msg-${System.currentTimeMillis()}"
                    }
                    
                    // Create temporary message for streaming display
                    val streamingMessage = com.github.brainhu.kirogui.model.ChatMessage(
                        id = assistantMessageId!!,
                        role = com.github.brainhu.kirogui.model.MessageRole.ASSISTANT,
                        content = contentBuilder.toString(),
                        timestamp = java.time.Instant.now(),
                        context = null
                    )
                    
                    // Update message area with streaming indicator
                    messageArea.addMessage(streamingMessage, isStreaming = !chunk.isComplete)
                }
            } catch (e: Exception) {
                // Show error notification
                com.intellij.notification.NotificationGroupManager.getInstance()
                    .getNotificationGroup("Kiro")
                    .createNotification(
                        "Failed to send message",
                        e.message ?: "Unknown error",
                        com.intellij.notification.NotificationType.ERROR
                    )
                    .notify(project)
            }
        }
    }
}
