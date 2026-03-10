package com.github.brainhu.kirogui.ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.FlowLayout
import javax.swing.JButton

/**
 * Bar for displaying attached context files/snippets as removable chips.
 *
 * Requirements: 2.7
 */
class ContextChipBar : JBPanel<ContextChipBar>(FlowLayout(FlowLayout.LEFT, 5, 5)) {

    private val chips = mutableListOf<ContextChip>()

    /**
     * Add a context chip with the given label.
     */
    fun addChip(label: String, contextData: Any? = null) {
        val chip = ContextChip(label, contextData) {
            removeChip(it)
        }
        chips.add(chip)
        add(chip)
        revalidate()
        repaint()
    }

    /**
     * Remove a specific chip.
     */
    private fun removeChip(chip: ContextChip) {
        chips.remove(chip)
        remove(chip)
        revalidate()
        repaint()
    }

    /**
     * Clear all chips.
     */
    fun clearChips() {
        chips.clear()
        removeAll()
        revalidate()
        repaint()
    }

    /**
     * Get all chip labels.
     */
    fun getChipLabels(): List<String> = chips.map { it.label }

    /**
     * Get all chip data.
     */
    fun getChipData(): List<Any?> = chips.map { it.contextData }
}

/**
 * Individual context chip with a label and close button.
 */
class ContextChip(
    val label: String,
    val contextData: Any?,
    private val onRemove: (ContextChip) -> Unit
) : JBPanel<ContextChip>(FlowLayout(FlowLayout.LEFT, 2, 2)) {

    init {
        val labelComponent = JBLabel(label)
        val closeButton = JButton("×").apply {
            toolTipText = "Remove context"
            isBorderPainted = false
            isContentAreaFilled = false
            addActionListener {
                onRemove(this@ContextChip)
            }
        }

        add(labelComponent)
        add(closeButton)

        // Visual styling
        border = javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(java.awt.Color.GRAY, 1, true),
            javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5)
        )
    }
}
