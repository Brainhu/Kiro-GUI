package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.AuthState
import com.github.brainhu.kirogui.model.ConnectionState
import com.github.brainhu.kirogui.service.AuthService
import com.github.brainhu.kirogui.service.KiroLspClient
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for [KiroStatusBarWidget].
 * 
 * Tests:
 * - Display text reflects connection and auth states
 * - Tooltip text provides detailed status information
 * - Click actions are available based on current state
 * - Widget properly observes state changes
 */
class KiroStatusBarWidgetTest : BasePlatformTestCase() {

    private lateinit var lspClient: KiroLspClient
    private lateinit var authService: AuthService
    private lateinit var connectionStateFlow: MutableStateFlow<ConnectionState>
    private lateinit var authStateFlow: MutableStateFlow<AuthState>

    override fun setUp() {
        super.setUp()
        
        // Mock LSP client
        lspClient = mockk(relaxed = true)
        connectionStateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
        every { lspClient.getConnectionState() } returns connectionStateFlow
        
        // Mock Auth service
        authService = mockk(relaxed = true)
        authStateFlow = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
        every { authService.getAuthState() } returns authStateFlow
        
        // Register mocked services
        project.registerService(KiroLspClient::class.java, lspClient)
        ApplicationManager.getApplication().registerService(AuthService::class.java, authService)
    }

    fun testWidgetDisplaysConnectedState() {
        connectionStateFlow.value = ConnectionState.CONNECTED
        authStateFlow.value = AuthState.Authenticated("testuser", "test@example.com")
        
        val widget = KiroStatusBarWidget(project)
        
        // Give coroutines time to update
        Thread.sleep(100)
        
        val displayText = widget.selectedValue
        assertTrue(displayText.contains("●"), "Should show connected icon")
        assertTrue(displayText.contains("testuser"), "Should show username")
    }

    fun testWidgetDisplaysConnectingState() {
        connectionStateFlow.value = ConnectionState.CONNECTING
        authStateFlow.value = AuthState.NotAuthenticated
        
        val widget = KiroStatusBarWidget(project)
        Thread.sleep(100)
        
        val displayText = widget.selectedValue
        assertTrue(displayText.contains("○"), "Should show connecting icon")
        assertTrue(displayText.contains("not logged in"), "Should show not logged in text")
    }

    fun testWidgetDisplaysFailedState() {
        connectionStateFlow.value = ConnectionState.FAILED
        authStateFlow.value = AuthState.NotAuthenticated
        
        val widget = KiroStatusBarWidget(project)
        Thread.sleep(100)
        
        val displayText = widget.selectedValue
        assertTrue(displayText.contains("✕"), "Should show failed icon")
    }

    fun testWidgetDisplaysReconnectingState() {
        connectionStateFlow.value = ConnectionState.RECONNECTING
        authStateFlow.value = AuthState.NotAuthenticated
        
        val widget = KiroStatusBarWidget(project)
        Thread.sleep(100)
        
        val displayText = widget.selectedValue
        assertTrue(displayText.contains("◐"), "Should show reconnecting icon")
    }

    fun testWidgetDisplaysAuthenticatedUser() {
        connectionStateFlow.value = ConnectionState.CONNECTED
        authStateFlow.value = AuthState.Authenticated("john.doe", "john@example.com")
        
        val widget = KiroStatusBarWidget(project)
        Thread.sleep(100)
        
        val displayText = widget.selectedValue
        assertTrue(displayText.contains("john.doe"), "Should show authenticated username")
    }

    fun testWidgetDisplaysTokenExpired() {
        connectionStateFlow.value = ConnectionState.CONNECTED
        authStateFlow.value = AuthState.TokenExpired
        
        val widget = KiroStatusBarWidget(project)
        Thread.sleep(100)
        
        val displayText = widget.selectedValue
        assertTrue(displayText.contains("token expired"), "Should show token expired text")
    }

    fun testWidgetDisplaysRefreshing() {
        connectionStateFlow.value = ConnectionState.CONNECTED
        authStateFlow.value = AuthState.Refreshing
        
        val widget = KiroStatusBarWidget(project)
        Thread.sleep(100)
        
        val displayText = widget.selectedValue
        assertTrue(displayText.contains("refreshing"), "Should show refreshing text")
    }

    fun testTooltipProvidesDetailedInfo() {
        connectionStateFlow.value = ConnectionState.CONNECTED
        authStateFlow.value = AuthState.Authenticated("testuser", "test@example.com")
        
        val widget = KiroStatusBarWidget(project)
        Thread.sleep(100)
        
        val tooltip = widget.tooltipText
        assertTrue(tooltip.contains("Connected"), "Tooltip should contain connection state")
        assertTrue(tooltip.contains("testuser"), "Tooltip should contain username")
    }

    fun testWidgetIdIsCorrect() {
        val widget = KiroStatusBarWidget(project)
        assertEquals("KiroStatusBarWidget", widget.ID())
    }

    fun testFactoryCreatesWidget() {
        val factory = KiroStatusBarWidgetFactory()
        assertEquals("KiroStatusBarWidget", factory.id)
        assertEquals("Kiro Status", factory.displayName)
        
        val widget = factory.createWidget(project)
        assertNotNull(widget)
        assertTrue(widget is KiroStatusBarWidget)
    }

    fun testFactoryIsAvailable() {
        val factory = KiroStatusBarWidgetFactory()
        assertTrue(factory.isAvailable(project))
    }

    override fun tearDown() {
        try {
            unmockkAll()
        } finally {
            super.tearDown()
        }
    }
}
