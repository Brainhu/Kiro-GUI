package com.github.brainhu.kirogui.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for the Kiro Hooks tool window (bottom panel).
 * Displays hook list with enable/disable toggles and execution log.
 *
 * Requirements: 5.3, 5.4
 */
class HookToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val hookPanel = HookPanel(project)
        val content = ContentFactory.getInstance().createContent(hookPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
