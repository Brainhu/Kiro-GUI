package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.service.SpecService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import java.awt.BorderLayout

/**
 * Right panel component with tabs for Requirements, Design, and Tasks phases.
 *
 * Requirements: 3.3, 3.4, 3.5, 3.6
 */
class PhaseTabBar(private val project: Project) : JBPanel<PhaseTabBar>(BorderLayout()) {
    private val specService = project.service<SpecService>()
    
    private val tabbedPane = JBTabbedPane()
    private val requirementsPanel = MarkdownPreviewPanel(project)
    private val designPanel = MarkdownPreviewPanel(project)
    private val tasksPanel = TaskChecklistPanel(project)
    
    private var currentSpecName: String? = null
    
    init {
        tabbedPane.addTab("📋 需求", requirementsPanel)
        tabbedPane.addTab("📐 设计", designPanel)
        tabbedPane.addTab("✅ 任务", tasksPanel)
        
        add(tabbedPane, BorderLayout.CENTER)
        
        // Wire task checklist updates
        tasksPanel.addTaskStatusListener { taskId, completed ->
            currentSpecName?.let { specName ->
                specService.updateTaskStatus(specName, taskId, completed)
            }
        }
    }
    
    fun loadSpec(specName: String) {
        currentSpecName = specName
        val spec = specService.getSpec(specName) ?: return
        
        // Load requirements
        requirementsPanel.setMarkdownContent(spec.requirements ?: "# 需求文档\n\n暂无内容")
        
        // Load design
        designPanel.setMarkdownContent(spec.design ?: "# 设计文档\n\n暂无内容")
        
        // Load tasks
        tasksPanel.loadTasks(specName, spec.tasks)
    }
}
