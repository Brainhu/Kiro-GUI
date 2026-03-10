package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.TaskItem
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Panel displaying task checklist with checkboxes that update task status.
 *
 * Requirements: 3.5, 3.6
 */
class TaskChecklistPanel(private val project: Project) : JBPanel<TaskChecklistPanel>(BorderLayout()) {
    private val taskPanel = JBPanel<JBPanel<*>>(GridBagLayout())
    private val scrollPane = JBScrollPane(taskPanel)
    
    private val taskStatusListeners = mutableListOf<(String, Boolean) -> Unit>()
    private var currentSpecName: String? = null
    
    init {
        add(scrollPane, BorderLayout.CENTER)
    }
    
    fun loadTasks(specName: String, tasks: List<TaskItem>) {
        currentSpecName = specName
        taskPanel.removeAll()
        
        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            anchor = GridBagConstraints.NORTHWEST
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = Insets(2, 5, 2, 5)
        }
        
        if (tasks.isEmpty()) {
            taskPanel.add(JLabel("暂无任务"), gbc)
        } else {
            addTaskItems(tasks, gbc, 0)
        }
        
        // Add filler at the bottom to push tasks to the top
        gbc.gridy++
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        taskPanel.add(JPanel(), gbc)
        
        taskPanel.revalidate()
        taskPanel.repaint()
    }
    
    private fun addTaskItems(tasks: List<TaskItem>, gbc: GridBagConstraints, indentLevel: Int) {
        for (task in tasks) {
            val checkbox = JBCheckBox(task.description, task.completed).apply {
                addActionListener {
                    val newStatus = isSelected
                    notifyTaskStatusChanged(task.id, newStatus)
                }
            }
            
            val taskRow = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(checkbox, BorderLayout.WEST)
            }
            
            // Add indentation for subtasks
            gbc.insets = Insets(2, 5 + (indentLevel * 20), 2, 5)
            taskPanel.add(taskRow, gbc)
            gbc.gridy++
            
            // Recursively add subtasks
            if (task.subtasks.isNotEmpty()) {
                addTaskItems(task.subtasks, gbc, indentLevel + 1)
            }
        }
    }
    
    fun addTaskStatusListener(listener: (String, Boolean) -> Unit) {
        taskStatusListeners.add(listener)
    }
    
    private fun notifyTaskStatusChanged(taskId: String, completed: Boolean) {
        taskStatusListeners.forEach { it(taskId, completed) }
    }
}
