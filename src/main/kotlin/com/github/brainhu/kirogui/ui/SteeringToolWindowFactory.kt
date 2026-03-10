package com.github.brainhu.kirogui.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for the Kiro Steering tool window (bottom panel).
 * Displays steering rule file list and Markdown editor with preview.
 *
 * Requirements: 6.2, 6.3
 */
class SteeringToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val steeringPanel = SteeringPanel(project)
        val content = ContentFactory.getInstance().createContent(steeringPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
