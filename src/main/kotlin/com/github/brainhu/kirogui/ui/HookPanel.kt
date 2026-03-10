package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.HookConfig
import com.github.brainhu.kirogui.model.HookExecutionRecord
import com.github.brainhu.kirogui.model.HookTrigger
import com.github.brainhu.kirogui.service.HookService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

/**
 * Main container panel for the Hook Tool Window.
 * Displays hook list with enable/disable toggles and execution log with timestamps.
 *
 * Requirements: 5.3, 5.4
 */
class HookPanel(private val project: Project) : JBPanel<HookPanel>(BorderLayout()) {
    private val log = Logger.getInstance(HookPanel::class.java)
    private val hookService = project.service<HookService>()
    
    private val hookTableModel = HookTableModel()
    private val hookTable = JBTable(hookTableModel)
    
    private val logTableModel = ExecutionLogTableModel()
    private val logTable = JBTable(logTableModel)
    
    init {
        // Create split pane with hook list on top and execution log on bottom
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = createHookListPanel()
            bottomComponent = createExecutionLogPanel()
            dividerLocation = 250
            resizeWeight = 0.4
        }
        
        add(splitPane, BorderLayout.CENTER)
        
        // Load initial data
        refresh()
    }
    
    private fun createHookListPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // Title label
        val titleLabel = JBLabel("Hook List").apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        panel.add(titleLabel, BorderLayout.NORTH)
        
        // Configure hook table
        hookTable.apply {
            setShowGrid(true)
            rowHeight = 30
            
            // Set column widths
            columnModel.getColumn(0).apply {
                preferredWidth = 50
                maxWidth = 50
            }
            columnModel.getColumn(1).preferredWidth = 200
            columnModel.getColumn(2).preferredWidth = 150
            columnModel.getColumn(3).preferredWidth = 100
            
            // Custom renderer for enabled column (checkbox)
            columnModel.getColumn(0).cellRenderer = CheckBoxRenderer()
            columnModel.getColumn(0).cellEditor = CheckBoxEditor()
        }
        
        val scrollPane = JBScrollPane(hookTable)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createExecutionLogPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // Title label
        val titleLabel = JBLabel("Execution Log").apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        panel.add(titleLabel, BorderLayout.NORTH)
        
        // Configure log table
        logTable.apply {
            setShowGrid(true)
            rowHeight = 25
            
            // Set column widths
            columnModel.getColumn(0).preferredWidth = 100
            columnModel.getColumn(1).apply {
                preferredWidth = 50
                maxWidth = 50
            }
            columnModel.getColumn(2).preferredWidth = 150
            columnModel.getColumn(3).preferredWidth = 300
            
            // Custom renderer for status column
            columnModel.getColumn(1).cellRenderer = StatusRenderer()
        }
        
        val scrollPane = JBScrollPane(logTable)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    /**
     * Refresh hook list and execution log from HookService.
     */
    fun refresh() {
        try {
            val hooks = hookService.listHooks()
            hookTableModel.setHooks(hooks)
            
            val logs = hookService.getExecutionLog()
            logTableModel.setLogs(logs)
        } catch (e: Exception) {
            log.error("Failed to refresh hook panel", e)
        }
    }
    
    // ── Table Models ────────────────────────────────────────────────────
    
    private inner class HookTableModel : AbstractTableModel() {
        private val columnNames = arrayOf("Enabled", "Name", "Trigger", "Status")
        private var hooks = listOf<HookConfig>()
        
        fun setHooks(newHooks: List<HookConfig>) {
            hooks = newHooks
            fireTableDataChanged()
        }
        
        override fun getRowCount(): Int = hooks.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]
        
        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> java.lang.Boolean::class.java
                else -> String::class.java
            }
        }
        
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0 // Only enabled column is editable
        }
        
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val hook = hooks[rowIndex]
            return when (columnIndex) {
                0 -> hook.enabled
                1 -> hook.name
                2 -> formatTrigger(hook.trigger)
                3 -> if (hook.enabled) "活跃" else "已禁用"
                else -> ""
            }
        }
        
        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 0 && aValue is Boolean) {
                val hook = hooks[rowIndex]
                try {
                    hookService.setHookEnabled(hook.id, aValue)
                    refresh() // Refresh to show updated state
                } catch (e: Exception) {
                    log.error("Failed to toggle hook '${hook.id}'", e)
                }
            }
        }
        
        private fun formatTrigger(trigger: HookTrigger): String {
            return when (trigger) {
                HookTrigger.FILE_SAVE -> "文件保存"
                HookTrigger.PRE_COMMIT -> "Git提交前"
                HookTrigger.FILE_CREATE -> "文件创建"
                HookTrigger.FILE_DELETE -> "文件删除"
            }
        }
    }
    
    private inner class ExecutionLogTableModel : AbstractTableModel() {
        private val columnNames = arrayOf("Timestamp", "Status", "Hook", "Message")
        private var logs = listOf<HookExecutionRecord>()
        
        fun setLogs(newLogs: List<HookExecutionRecord>) {
            // Show most recent first
            logs = newLogs.reversed()
            fireTableDataChanged()
        }
        
        override fun getRowCount(): Int = logs.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]
        
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val log = logs[rowIndex]
            return when (columnIndex) {
                0 -> formatTimestamp(log.timestamp)
                1 -> if (log.success) "✅" else "❌"
                2 -> log.hookId
                3 -> log.message
                else -> ""
            }
        }
        
        private fun formatTimestamp(instant: java.time.Instant): String {
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            return formatter.format(instant.atZone(java.time.ZoneId.systemDefault()))
        }
    }
    
    // ── Custom Renderers and Editors ────────────────────────────────────
    
    private class CheckBoxRenderer : JBCheckBox(), TableCellRenderer {
        init {
            horizontalAlignment = SwingConstants.CENTER
        }
        
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            isSelected = value as? Boolean ?: false
            return this
        }
    }
    
    private class CheckBoxEditor : DefaultCellEditor(JBCheckBox()) {
        init {
            (editorComponent as JBCheckBox).horizontalAlignment = SwingConstants.CENTER
        }
    }
    
    private class StatusRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = SwingConstants.CENTER
        }
    }
}
