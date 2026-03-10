package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.service.SpecService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Main container panel for the Spec Tool Window.
 * Uses a JSplitPane layout with SpecTree on the left and PhaseTabBar on the right.
 *
 * Requirements: 3.1, 3.3, 3.4, 3.5, 3.6
 */
class SpecPanel(private val project: Project) : JBPanel<SpecPanel>(BorderLayout()) {
    private val specService = project.service<SpecService>()
    
    private val specTree = SpecTree(project)
    private val phaseTabBar = PhaseTabBar(project)
    private val actionBar = createActionBar()
    
    init {
        val splitter = JBSplitter(false, 0.3f).apply {
            firstComponent = specTree
            secondComponent = phaseTabBar
        }
        
        add(splitter, BorderLayout.CENTER)
        add(actionBar, BorderLayout.SOUTH)
        
        // Wire spec selection to phase tab bar
        specTree.addSelectionListener { specName ->
            phaseTabBar.loadSpec(specName)
        }
    }
    
    private fun createActionBar(): JPanel {
        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val buttonPanel = JBPanel<JBPanel<*>>().apply {
                add(JButton("▶ 执行当前任务").apply {
                    addActionListener {
                        // TODO: Wire to task execution
                    }
                })
                add(JButton("⏭ 下一阶段").apply {
                    addActionListener {
                        // TODO: Wire to phase transition
                    }
                })
            }
            add(buttonPanel, BorderLayout.WEST)
        }
    }
    
    fun refresh() {
        specTree.refresh()
    }
}
