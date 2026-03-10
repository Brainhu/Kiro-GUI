package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.service.ChatService
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import javax.swing.JButton
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.FlowLayout

/**
 * Tab bar for managing multiple chat sessions.
 * Uses JBTabbedPane with a "+" button to create new sessions.
 *
 * Requirements: 2.6
 */
class SessionTabBar(
    private val project: Project,
    private val chatService: ChatService
) : JPanel(BorderLayout()) {

    private val tabbedPane = JBTabbedPane()
    private val newSessionButton = JButton("+")
    private val sessionChangeListeners = mutableListOf<(String) -> Unit>()

    init {
        // Add existing sessions to tabs
        refreshTabs()

        // Listen for tab changes
        tabbedPane.addChangeListener {
            val sessionId = getCurrentSessionId()
            if (sessionId != null) {
                notifySessionChange(sessionId)
            }
        }

        // New session button
        newSessionButton.toolTipText = "Create new chat session"
        newSessionButton.addActionListener {
            val newSession = chatService.createSession()
            addSessionTab(newSession.id, newSession.title)
        }

        // Layout: tabs on left, new button on right
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
            add(newSessionButton)
        }

        add(tabbedPane, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.EAST)
    }

    /**
     * Refresh tabs from all active sessions.
     */
    private fun refreshTabs() {
        tabbedPane.removeAll()
        val sessions = chatService.getAllSessions()
        sessions.forEach { session ->
            addSessionTab(session.id, session.title)
        }
    }

    /**
     * Add a new tab for the given session.
     */
    private fun addSessionTab(sessionId: String, title: String) {
        // Tab content is just a placeholder - actual messages are rendered in MessageArea
        val tabContent = JPanel().apply {
            add(javax.swing.JLabel("Session: $sessionId"))
        }
        tabbedPane.addTab(title, tabContent)
        tabbedPane.selectedIndex = tabbedPane.tabCount - 1
    }

    /**
     * Get the currently selected session ID.
     */
    fun getCurrentSessionId(): String? {
        val selectedIndex = tabbedPane.selectedIndex
        if (selectedIndex < 0) return null
        val sessions = chatService.getAllSessions()
        return sessions.getOrNull(selectedIndex)?.id
    }

    /**
     * Add a listener for session changes.
     */
    fun addSessionChangeListener(listener: (String) -> Unit) {
        sessionChangeListeners.add(listener)
    }

    /**
     * Notify all listeners of a session change.
     */
    private fun notifySessionChange(sessionId: String) {
        sessionChangeListeners.forEach { it(sessionId) }
    }
}
