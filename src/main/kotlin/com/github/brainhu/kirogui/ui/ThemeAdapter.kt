package com.github.brainhu.kirogui.ui

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color

/**
 * Theme adapter utility for JCEF panels.
 * Detects current IDE theme, generates CSS variables, and listens for theme changes.
 *
 * Requirements: 9.1, 9.2, 9.3, 9.4
 */
class ThemeAdapter {
    
    private val logger = Logger.getInstance(ThemeAdapter::class.java)
    private val themeChangeListeners = mutableListOf<() -> Unit>()
    
    init {
        // Register theme change listener
        ApplicationManager.getApplication().messageBus
            .connect()
            .subscribe(LafManagerListener.TOPIC, LafManagerListener {
                logger.info("IDE theme changed, notifying listeners")
                notifyThemeChanged()
            })
    }
    
    /**
     * Detects if the current IDE theme is dark.
     */
    fun isDarkTheme(): Boolean {
        return JBColor.isBright().not()
    }
    
    /**
     * Generates CSS variables matching the current IDE color scheme.
     */
    fun generateCssVariables(): Map<String, String> {
        val isDark = isDarkTheme()
        val editorScheme = EditorColorsManager.getInstance().globalScheme
        
        // Get IDE colors
        val backgroundColor = UIUtil.getPanelBackground()
        val textColor = UIUtil.getLabelForeground()
        val borderColor = JBColor.border()
        val linkColor = JBColor.link()
        
        // Get editor colors for code blocks
        val editorBackground = editorScheme.defaultBackground
        val editorForeground = editorScheme.defaultForeground
        
        return mapOf(
            "--bg-color" to backgroundColor.toCssColor(),
            "--text-color" to textColor.toCssColor(),
            "--border-color" to borderColor.toCssColor(),
            "--link-color" to linkColor.toCssColor(),
            
            // Message-specific colors
            "--user-bg" to if (isDark) "#1a237e" else "#e3f2fd",
            "--assistant-bg" to if (isDark) "#263238" else "#f5f5f5",
            
            // Code block colors from editor scheme
            "--code-bg" to editorBackground.toCssColor(),
            "--code-fg" to editorForeground.toCssColor(),
            "--code-border" to if (isDark) "#404040" else "#d0d0d0",
            
            // Button colors
            "--button-bg" to if (isDark) "#1976d2" else "#2196f3",
            "--button-hover" to if (isDark) "#1565c0" else "#1976d2",
            "--button-text" to "#ffffff",
            
            // Streaming indicator
            "--streaming-color" to if (isDark) "#66bb6a" else "#4caf50",
            
            // Syntax highlighting colors from editor scheme
            "--syntax-keyword" to (editorScheme.getAttributes(com.intellij.openapi.editor.DefaultLanguageHighlighterColors.KEYWORD)?.foregroundColor ?: textColor).toCssColor(),
            "--syntax-string" to (editorScheme.getAttributes(com.intellij.openapi.editor.DefaultLanguageHighlighterColors.STRING)?.foregroundColor ?: textColor).toCssColor(),
            "--syntax-number" to (editorScheme.getAttributes(com.intellij.openapi.editor.DefaultLanguageHighlighterColors.NUMBER)?.foregroundColor ?: textColor).toCssColor(),
            "--syntax-comment" to (editorScheme.getAttributes(com.intellij.openapi.editor.DefaultLanguageHighlighterColors.LINE_COMMENT)?.foregroundColor ?: textColor).toCssColor(),
            "--syntax-function" to (editorScheme.getAttributes(com.intellij.openapi.editor.DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)?.foregroundColor ?: textColor).toCssColor()
        )
    }
    
    /**
     * Generates CSS string from variables map.
     */
    fun generateCssString(): String {
        val variables = generateCssVariables()
        return variables.entries.joinToString("\n") { (key, value) ->
            "  $key: $value;"
        }
    }
    
    /**
     * Gets the appropriate highlight.js theme URL based on current IDE theme.
     */
    fun getHighlightJsTheme(): String {
        return if (isDarkTheme()) {
            "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css"
        } else {
            "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css"
        }
    }
    
    /**
     * Registers a listener to be notified when the theme changes.
     */
    fun addThemeChangeListener(listener: () -> Unit) {
        themeChangeListeners.add(listener)
    }
    
    /**
     * Removes a theme change listener.
     */
    fun removeThemeChangeListener(listener: () -> Unit) {
        themeChangeListeners.remove(listener)
    }
    
    private fun notifyThemeChanged() {
        ApplicationManager.getApplication().invokeLater {
            themeChangeListeners.forEach { it.invoke() }
        }
    }
    
    /**
     * Converts a Color to CSS color string (hex format).
     */
    private fun Color.toCssColor(): String {
        return String.format("#%02x%02x%02x", red, green, blue)
    }
    
    companion object {
        private var instance: ThemeAdapter? = null
        
        /**
         * Gets the singleton instance of ThemeAdapter.
         */
        fun getInstance(): ThemeAdapter {
            if (instance == null) {
                instance = ThemeAdapter()
            }
            return instance!!
        }
    }
}
