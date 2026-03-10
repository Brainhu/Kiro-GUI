package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.SteeringRuleFile
import com.github.brainhu.kirogui.service.SteeringService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.*

/**
 * Main container panel for the Steering Config Tool Window.
 * Left panel: rule file list with new file button
 * Right panel: Markdown editor with save/preview/undo actions
 *
 * Requirements: 6.2, 6.3
 */
class SteeringPanel(private val project: Project) : JBPanel<SteeringPanel>(BorderLayout()) {
    private val log = Logger.getInstance(SteeringPanel::class.java)
    private val steeringService = project.service<SteeringService>()
    
    private val fileListModel = DefaultListModel<SteeringRuleFile>()
    private val fileList = JBList(fileListModel)
    
    private val editorArea = JTextArea()
    private val previewPanel = MarkdownPreviewPanel(project)
    
    private val cardLayout = CardLayout()
    private val contentPanel = JBPanel<JBPanel<*>>(cardLayout)
    
    private var currentFile: SteeringRuleFile? = null
    private var isPreviewMode = false
    private var originalContent: String = ""
    
    private val saveButton = JButton("💾 保存")
    private val previewButton = JButton("👁 预览")
    private val undoButton = JButton("↩ 撤销")
    
    init {
        val splitter = JBSplitter(false, 0.3f).apply {
            firstComponent = createLeftPanel()
            secondComponent = createRightPanel()
        }
        
        add(splitter, BorderLayout.CENTER)
        add(createStatusBar(), BorderLayout.SOUTH)
        
        // Wire file selection
        fileList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val selected = fileList.selectedValue
                if (selected != null) {
                    loadFile(selected)
                }
            }
        }
        
        // Wire action buttons
        saveButton.addActionListener { saveCurrentFile() }
        previewButton.addActionListener { togglePreview() }
        undoButton.addActionListener { undoChanges() }
        
        // Wire document change listener to update button states
        editorArea.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateButtonStates()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateButtonStates()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateButtonStates()
        })
        
        // Load initial data
        refresh()
        
        // Start watching for file changes
        steeringService.watchForChanges()
    }
    
    private fun createLeftPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // Title label
        val titleLabel = JBLabel("Rule Files").apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        panel.add(titleLabel, BorderLayout.NORTH)
        
        // File list
        fileList.apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = RuleFileListCellRenderer()
        }
        
        val scrollPane = JBScrollPane(fileList)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // New file button
        val newFileButton = JButton("＋ 新建").apply {
            addActionListener { createNewFile() }
        }
        val buttonPanel = JBPanel<JBPanel<*>>().apply {
            add(newFileButton)
        }
        panel.add(buttonPanel, BorderLayout.SOUTH)
        
        return panel
    }
    
    private fun createRightPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // Title label
        val titleLabel = JBLabel("Rule Editor").apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        panel.add(titleLabel, BorderLayout.NORTH)
        
        // Content panel with card layout (editor or preview)
        editorArea.apply {
            lineWrap = true
            wrapStyleWord = true
            font = font.deriveFont(14f)
        }
        
        val editorScrollPane = JBScrollPane(editorArea)
        contentPanel.add(editorScrollPane, "editor")
        contentPanel.add(previewPanel, "preview")
        
        panel.add(contentPanel, BorderLayout.CENTER)
        
        // Action bar
        val actionBar = JBPanel<JBPanel<*>>().apply {
            add(saveButton)
            add(previewButton)
            add(undoButton)
        }
        panel.add(actionBar, BorderLayout.SOUTH)
        
        // Initially disable buttons
        updateButtonStates()
        
        return panel
    }
    
    private fun createStatusBar(): JComponent {
        return JBPanel<JBPanel<*>>().apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(JBLabel("规则文件变更将自动重新加载，无需重启插件"))
        }
    }
    
    private fun loadFile(file: SteeringRuleFile) {
        try {
            val content = steeringService.getRuleContent(file.name)
            editorArea.text = content
            originalContent = content
            currentFile = file
            
            // Switch to editor mode
            isPreviewMode = false
            cardLayout.show(contentPanel, "editor")
            previewButton.text = "👁 预览"
            
            updateButtonStates()
        } catch (e: Exception) {
            log.error("Failed to load rule file: ${file.name}", e)
            JOptionPane.showMessageDialog(
                this,
                "Failed to load file: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    private fun saveCurrentFile() {
        val file = currentFile ?: return
        
        try {
            val content = editorArea.text
            steeringService.saveRuleContent(file.name, content)
            originalContent = content
            
            log.info("Saved steering rule file: ${file.name}")
            JOptionPane.showMessageDialog(
                this,
                "File saved successfully",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            )
            
            updateButtonStates()
        } catch (e: Exception) {
            log.error("Failed to save rule file: ${file.name}", e)
            JOptionPane.showMessageDialog(
                this,
                "Failed to save file: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    private fun togglePreview() {
        if (isPreviewMode) {
            // Switch to editor
            cardLayout.show(contentPanel, "editor")
            previewButton.text = "👁 预览"
            isPreviewMode = false
        } else {
            // Switch to preview
            val content = editorArea.text
            previewPanel.setMarkdownContent(content)
            cardLayout.show(contentPanel, "preview")
            previewButton.text = "✏ 编辑"
            isPreviewMode = true
        }
    }
    
    private fun undoChanges() {
        editorArea.text = originalContent
        
        // Switch to editor mode if in preview
        if (isPreviewMode) {
            cardLayout.show(contentPanel, "editor")
            previewButton.text = "👁 预览"
            isPreviewMode = false
        }
        
        updateButtonStates()
    }
    
    private fun createNewFile() {
        val fileName = JOptionPane.showInputDialog(
            this,
            "Enter new rule file name (without .md extension):",
            "New Rule File",
            JOptionPane.PLAIN_MESSAGE
        )
        
        if (fileName.isNullOrBlank()) {
            return
        }
        
        val fullFileName = if (fileName.endsWith(".md")) fileName else "$fileName.md"
        
        try {
            val newFile = steeringService.createRuleFile(fullFileName)
            refresh()
            
            // Select the new file
            val index = fileListModel.toArray().indexOfFirst { 
                (it as SteeringRuleFile).name == newFile.name 
            }
            if (index >= 0) {
                fileList.selectedIndex = index
            }
        } catch (e: Exception) {
            log.error("Failed to create rule file: $fullFileName", e)
            JOptionPane.showMessageDialog(
                this,
                "Failed to create file: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    private fun updateButtonStates() {
        val hasFile = currentFile != null
        val hasChanges = hasFile && editorArea.text != originalContent
        
        saveButton.isEnabled = hasChanges
        previewButton.isEnabled = hasFile
        undoButton.isEnabled = hasChanges
    }
    
    fun refresh() {
        try {
            val files = steeringService.listRuleFiles()
            fileListModel.clear()
            files.forEach { fileListModel.addElement(it) }
        } catch (e: Exception) {
            log.error("Failed to refresh steering panel", e)
        }
    }
    
    fun dispose() {
        steeringService.stopWatching()
        previewPanel.dispose()
    }
    
    // ── Custom Cell Renderer ────────────────────────────────────────────
    
    private class RuleFileListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            
            if (value is SteeringRuleFile) {
                text = "📄 ${value.name}"
            }
            
            return component
        }
    }
}
