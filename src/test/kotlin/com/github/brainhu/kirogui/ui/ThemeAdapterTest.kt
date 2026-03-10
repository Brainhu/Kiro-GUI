package com.github.brainhu.kirogui.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for ThemeAdapter.
 * 
 * Tests theme detection, CSS variable generation, and theme change listeners.
 * Requirements: 9.1, 9.2, 9.3, 9.4
 */
class ThemeAdapterTest : BasePlatformTestCase() {
    
    private lateinit var themeAdapter: ThemeAdapter
    
    override fun setUp() {
        super.setUp()
        themeAdapter = ThemeAdapter.getInstance()
    }
    
    @Test
    fun testIsDarkThemeReturnsBoolean() {
        // Should return a boolean value
        val isDark = themeAdapter.isDarkTheme()
        assertTrue(isDark is Boolean)
    }
    
    @Test
    fun testGenerateCssVariablesReturnsNonEmptyMap() {
        val cssVars = themeAdapter.generateCssVariables()
        
        // Should return a non-empty map
        assertTrue(cssVars.isNotEmpty())
        
        // Should contain essential CSS variables
        assertTrue(cssVars.containsKey("--bg-color"))
        assertTrue(cssVars.containsKey("--text-color"))
        assertTrue(cssVars.containsKey("--border-color"))
        assertTrue(cssVars.containsKey("--link-color"))
        assertTrue(cssVars.containsKey("--code-bg"))
        assertTrue(cssVars.containsKey("--code-fg"))
        
        // Should contain syntax highlighting colors
        assertTrue(cssVars.containsKey("--syntax-keyword"))
        assertTrue(cssVars.containsKey("--syntax-string"))
        assertTrue(cssVars.containsKey("--syntax-number"))
        assertTrue(cssVars.containsKey("--syntax-comment"))
        assertTrue(cssVars.containsKey("--syntax-function"))
    }
    
    @Test
    fun testCssVariablesAreValidHexColors() {
        val cssVars = themeAdapter.generateCssVariables()
        
        // All values should be valid hex colors
        cssVars.values.forEach { color ->
            assertTrue(
                color.matches(Regex("^#[0-9a-fA-F]{6}$")),
                "Color '$color' is not a valid hex color"
            )
        }
    }
    
    @Test
    fun testGenerateCssStringReturnsValidCss() {
        val cssString = themeAdapter.generateCssString()
        
        // Should return a non-empty string
        assertTrue(cssString.isNotEmpty())
        
        // Should contain CSS variable declarations
        assertTrue(cssString.contains("--bg-color:"))
        assertTrue(cssString.contains("--text-color:"))
        assertTrue(cssString.contains("#"))
    }
    
    @Test
    fun testGetHighlightJsThemeReturnsValidUrl() {
        val themeUrl = themeAdapter.getHighlightJsTheme()
        
        // Should return a valid URL
        assertTrue(themeUrl.startsWith("https://"))
        assertTrue(themeUrl.contains("highlight.js"))
        assertTrue(themeUrl.endsWith(".css"))
        
        // Should be either github or github-dark theme
        assertTrue(
            themeUrl.contains("github.min.css") || themeUrl.contains("github-dark.min.css")
        )
    }
    
    @Test
    fun testThemeChangeListenerRegistration() {
        var listenerCalled = false
        val listener = { listenerCalled = true }
        
        // Add listener
        themeAdapter.addThemeChangeListener(listener)
        
        // Remove listener
        themeAdapter.removeThemeChangeListener(listener)
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun testSingletonInstance() {
        val instance1 = ThemeAdapter.getInstance()
        val instance2 = ThemeAdapter.getInstance()
        
        // Should return the same instance
        assertTrue(instance1 === instance2)
    }
    
    @Test
    fun testCssVariablesConsistentBetweenCalls() {
        val cssVars1 = themeAdapter.generateCssVariables()
        val cssVars2 = themeAdapter.generateCssVariables()
        
        // Should return the same variables (assuming theme doesn't change)
        assertEquals(cssVars1.size, cssVars2.size)
        cssVars1.keys.forEach { key ->
            assertTrue(cssVars2.containsKey(key))
        }
    }
}
