package com.github.brainhu.kirogui.actions

import com.github.brainhu.kirogui.service.ChatService
import com.github.brainhu.kirogui.service.ContextService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Action to send selected code to the Kiro Chat panel.
 * Opens the Chat tool window and attaches the selected code as context.
 * Shortcut: Ctrl+K K
 * 
 * Requirements: 7.1, 7.2, 7.4
 */
class SendToChatAction : AnAction() {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // Get services
        val contextService = project.service<ContextService>()
        val chatService = project.service<ChatService>()
        
        // Collect editor context
        val context = contextService.collectContext(editor)
        
        // Open Chat tool window
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val chatToolWindow = toolWindowManager.getToolWindow("Kiro Chat")
        
        if (chatToolWindow != null) {
            // Activate the tool window (show it if hidden)
            chatToolWindow.activate(null)
            
            // Get or create session
            val sessions = chatService.getAllSessions()
            val session = if (sessions.isEmpty()) {
                chatService.createSession()
            } else {
                sessions.first()
            }
            
            // Build message with selected code
            val selectedText = context.selectedText ?: return
            val filePath = context.filePath ?: "Unknown file"
            val message = "I have selected the following code from $filePath:\n\n```${context.language ?: ""}\n$selectedText\n```"
            
            // Send message asynchronously
            scope.launch {
                chatService.sendMessage(session.id, message, context)
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabled = hasSelection
    }
}
