package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.service.ChatService
import com.github.brainhu.kirogui.service.ContextService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for the Kiro Chat tool window (right side panel).
 * Registered in plugin.xml as a right-side tool window with dock/float/minimize support.
 *
 * Requirements: 2.1, 2.2, 2.6, 2.7
 */
class ChatToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Get services
        val chatService = project.service<ChatService>()
        val contextService = project.service<ContextService>()

        // Create chat panel
        val chatPanel = ChatPanel(project, chatService, contextService)

        // Add to tool window
        val content = ContentFactory.getInstance().createContent(chatPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
