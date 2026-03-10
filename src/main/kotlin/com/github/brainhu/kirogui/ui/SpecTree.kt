package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.SpecPhase
import com.github.brainhu.kirogui.model.SpecSummary
import com.github.brainhu.kirogui.service.SpecService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.event.TreeSelectionListener

/**
 * Left panel component displaying the spec tree list with status indicators.
 *
 * Requirements: 3.1
 */
class SpecTree(private val project: Project) : JBPanel<SpecTree>(BorderLayout()) {
    private val specService = project.service<SpecService>()
    
    private val root = DefaultMutableTreeNode("Specs")
    private val treeModel = DefaultTreeModel(root)
    private val tree = Tree(treeModel)
    
    private val statusPanel = JBPanel<JBPanel<*>>().apply {
        layout = BorderLayout()
        add(JLabel("Status"), BorderLayout.NORTH)
    }
    
    init {
        // Tree setup
        tree.isRootVisible = true
        tree.showsRootHandles = true
        
        val scrollPane = JBScrollPane(tree)
        add(scrollPane, BorderLayout.CENTER)
        
        // New spec button
        val newSpecButton = JButton("＋ 新建").apply {
            addActionListener {
                // TODO: Show dialog to create new spec
            }
        }
        
        val bottomPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(newSpecButton, BorderLayout.NORTH)
            add(statusPanel, BorderLayout.CENTER)
        }
        
        add(bottomPanel, BorderLayout.SOUTH)
        
        // Load initial data
        refresh()
        
        // Update status panel on selection
        tree.addTreeSelectionListener {
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
            val spec = node?.userObject as? SpecSummary
            if (spec != null) {
                updateStatusPanel(spec)
            }
        }
    }
    
    fun refresh() {
        root.removeAllChildren()
        val specs = specService.listSpecs()
        
        for (spec in specs) {
            val node = DefaultMutableTreeNode(spec)
            root.add(node)
        }
        
        treeModel.reload()
        
        // Expand root
        tree.expandRow(0)
    }
    
    private fun updateStatusPanel(spec: SpecSummary) {
        statusPanel.removeAll()
        statusPanel.add(JLabel("Status"), BorderLayout.NORTH)
        
        val phaseIcon = when (spec.phase) {
            SpecPhase.REQUIREMENTS -> "📋"
            SpecPhase.DESIGN -> "📐"
            SpecPhase.TASKS -> "✅"
            SpecPhase.COMPLETED -> "✅"
        }
        
        val statusText = """
            <html>
            <div style='padding: 5px;'>
            <p>需求: ${if (spec.phase.ordinal >= SpecPhase.REQUIREMENTS.ordinal) "✅" else "⏳"}</p>
            <p>设计: ${if (spec.phase.ordinal >= SpecPhase.DESIGN.ordinal) "✅" else "⏳"}</p>
            <p>任务: ${if (spec.phase.ordinal >= SpecPhase.TASKS.ordinal) "✅" else "⏳"}</p>
            <p>进度: ${spec.taskProgress.completed}/${spec.taskProgress.total}</p>
            </div>
            </html>
        """.trimIndent()
        
        statusPanel.add(JLabel(statusText), BorderLayout.CENTER)
        statusPanel.revalidate()
        statusPanel.repaint()
    }
    
    fun addSelectionListener(listener: (String) -> Unit) {
        tree.addTreeSelectionListener(TreeSelectionListener { e ->
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
            val spec = node?.userObject as? SpecSummary
            if (spec != null) {
                listener(spec.name)
            }
        })
    }
}
