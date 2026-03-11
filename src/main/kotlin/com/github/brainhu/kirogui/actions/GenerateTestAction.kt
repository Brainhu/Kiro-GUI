package com.github.brainhu.kirogui.actions

import com.github.brainhu.kirogui.service.ChatService
import com.github.brainhu.kirogui.service.ContextService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Action to generate tests for selected code via Kiro AI.
 * Shortcut: Ctrl+K T
 * 
 * Requirements: 7.1, 7.2
 */
class GenerateTestAction : AnAction() {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // Get services
        val contextService = project.service<ContextService>()
        val chatService = project.service<ChatService>()
        
        // Collect editor context
        val context = contextService.collectContext(editor)
        
        // Build test generation prompt
        val selectedText = context.selectedText ?: return
        val message = "Please generate unit tests for the following code:\n\n```${context.language ?: ""}\n$selectedText\n```"
        
        // Get or create session
        val sessions = chatService.getAllSessions()
        val session = if (sessions.isEmpty()) {
            chatService.createSession()
        } else {
            sessions.first()
        }
        
        // Send message asynchronously
        scope.launch {
            try {
                chatService.sendMessage(session.id, message, context)
                
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Kiro")
                    .createNotification(
                        "Kiro",
                        "Request sent to Kiro AI. Check the Chat panel for response.",
                        NotificationType.INFORMATION
                    )
                    .notify(project)
            } catch (e: IllegalStateException) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Kiro")
                    .createNotification(
                        "Kiro LSP Server Not Connected",
                        "Please ensure the Kiro LSP server is running and connected.",
                        NotificationType.WARNING
                    )
                    .notify(project)
            } catch (e: Exception) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Kiro")
                    .createNotification(
                        "Kiro Error",
                        "Failed to send request: ${e.message}",
                        NotificationType.ERROR
                    )
                    .notify(project)
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabled = hasSelection
    }
}
