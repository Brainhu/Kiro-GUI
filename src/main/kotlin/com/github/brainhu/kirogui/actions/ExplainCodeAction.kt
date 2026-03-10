package com.github.brainhu.kirogui.actions

import com.github.brainhu.kirogui.service.ChatService
import com.github.brainhu.kirogui.service.ContextService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Action to explain selected code via Kiro AI.
 * Shortcut: Ctrl+K E
 * 
 * Requirements: 7.1, 7.2
 */
class ExplainCodeAction : AnAction() {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // Get services
        val contextService = project.service<ContextService>()
        val chatService = project.service<ChatService>()
        
        // Collect editor context
        val context = contextService.collectContext(editor)
        
        // Build explain prompt
        val selectedText = context.selectedText ?: return
        val message = "Please explain the following code:\n\n```${context.language ?: ""}\n$selectedText\n```"
        
        // Get or create session
        val sessions = chatService.getAllSessions()
        val session = if (sessions.isEmpty()) {
            chatService.createSession()
        } else {
            sessions.first()
        }
        
        // Send message asynchronously
        scope.launch {
            chatService.sendMessage(session.id, message, context)
        }
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabled = hasSelection
    }
}
