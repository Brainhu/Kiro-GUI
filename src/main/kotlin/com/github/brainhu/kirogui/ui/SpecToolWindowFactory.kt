package com.github.brainhu.kirogui.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for the Kiro Specs tool window (bottom panel).
 * 
 * Requirements: 3.1, 3.3, 3.4, 3.5, 3.6
 */
class SpecToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val specPanel = SpecPanel(project)
        val content = ContentFactory.getInstance().createContent(specPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
