package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.service.ChatService
import com.github.brainhu.kirogui.service.ContextService
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanel
import kotlinx.coroutines.*
import java.awt.BorderLayout

/**
 * Main container for the Kiro Chat tool window.
 * Uses BorderLayout: SessionTabBar at top, MessageArea in center, ContextChipBar and InputArea at bottom.
 *
 * Requirements: 2.1, 2.2, 2.6, 2.7
 */
class ChatPanel(
    private val project: Project,
    private val chatService: ChatService,
    private val contextService: ContextService
) : JBPanel<ChatPanel>(BorderLayout()), Disposable {

    private val sessionTabBar: SessionTabBar
    private val messageArea: MessageArea
    private val contextChipBar: ContextChipBar
    private val inputArea: InputArea
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Initialize components
        sessionTabBar = SessionTabBar(project, chatService)
        messageArea = MessageArea(project)
        contextChipBar = ContextChipBar()
        inputArea = InputArea(project, chatService, contextService, contextChipBar, messageArea)

        // Register message area for disposal
        Disposer.register(this, Disposable { messageArea.dispose() })

        // Layout: top = tabs, center = message area (JCEF), bottom = context + input
        add(sessionTabBar, BorderLayout.NORTH)
        add(messageArea, BorderLayout.CENTER)

        // Bottom panel contains context chips and input area
        val bottomPanel = JBPanel<Nothing>(BorderLayout()).apply {
            add(contextChipBar, BorderLayout.NORTH)
            add(inputArea, BorderLayout.CENTER)
        }
        add(bottomPanel, BorderLayout.SOUTH)

        // Create initial session
        chatService.createSession()
        
        // Load messages for current session
        loadCurrentSessionMessages()
        
        // Listen for session changes to reload messages
        sessionTabBar.addSessionChangeListener { sessionId ->
            loadSessionMessages(sessionId)
        }
    }

    private fun loadCurrentSessionMessages() {
        val sessions = chatService.getAllSessions()
        if (sessions.isNotEmpty()) {
            loadSessionMessages(sessions.first().id)
        }
    }

    private fun loadSessionMessages(sessionId: String) {
        coroutineScope.launch {
            val session = chatService.getSession(sessionId)
            if (session != null) {
                messageArea.clearMessages()
                session.messages.forEach { message ->
                    messageArea.addMessage(message, isStreaming = false)
                }
            }
        }
    }

    /**
     * Provides access to the message area for displaying streaming responses.
     */
    fun getMessageArea(): MessageArea = messageArea

    override fun dispose() {
        coroutineScope.cancel()
    }
}
