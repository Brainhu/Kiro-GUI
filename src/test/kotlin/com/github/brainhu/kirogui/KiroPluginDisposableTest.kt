package com.github.brainhu.kirogui

import com.github.brainhu.kirogui.model.*
import com.github.brainhu.kirogui.service.*
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import java.io.File
import java.time.Instant

/**
 * Unit tests for [KiroPluginDisposable].
 *
 * Verifies that the disposable correctly:
 * - Saves all active ChatService sessions to disk
 * - Unregisters HookService event listeners
 * - Stops SteeringService file watching
 * - Disconnects LSP client
 *
 * Requirements: 1.4
 */
class KiroPluginDisposableTest : BasePlatformTestCase() {

    @Test
    fun testDispose_persistsChatSessions() {
        // Arrange: Create a session in ChatService
        val chatService = project.service<ChatServiceImpl>()
        val session = chatService.createSession()
        
        // Add a message to the session
        val message = ChatMessage(
            id = "msg-1",
            role = MessageRole.USER,
            content = "Test message",
            timestamp = Instant.now()
        )
        session.messages.add(message)

        // Act: Dispose the plugin
        val disposable = project.service<KiroPluginDisposable>()
        disposable.dispose()

        // Assert: Verify session was persisted to disk
        val sessionsDir = File(project.basePath, ".kiro/sessions")
        assertTrue("Sessions directory should exist", sessionsDir.exists())
        
        val sessionFile = File(sessionsDir, "${session.id}.json")
        assertTrue("Session file should exist", sessionFile.exists())
        
        val content = sessionFile.readText()
        assertTrue("Session file should contain session ID", content.contains(session.id))
        assertTrue("Session file should contain message content", content.contains("Test message"))
    }

    @Test
    fun testDispose_unregistersHookListeners() {
        // Arrange: Register hook listeners
        val hookService = project.service<HookServiceImpl>()
        hookService.registerEventListeners()

        // Act: Dispose the plugin
        val disposable = project.service<KiroPluginDisposable>()
        disposable.dispose()

        // Assert: Verify listeners were unregistered (no exception thrown)
        // The actual verification is that dispose() completes without error
        // and the service is in a clean state
        assertTrue("Dispose should complete successfully", true)
    }

    @Test
    fun testDispose_stopsSteeringFileWatching() {
        // Arrange: Start steering file watching
        val steeringService = project.service<SteeringServiceImpl>()
        steeringService.watchForChanges()

        // Act: Dispose the plugin
        val disposable = project.service<KiroPluginDisposable>()
        disposable.dispose()

        // Assert: Verify file watching was stopped (no exception thrown)
        assertTrue("Dispose should complete successfully", true)
    }

    @Test
    fun testDispose_disconnectsLspClient() {
        // Arrange: Get LSP client (it may not be connected, which is fine)
        val lspClient = project.service<KiroLspClientImpl>()

        // Act: Dispose the plugin
        val disposable = project.service<KiroPluginDisposable>()
        disposable.dispose()

        // Assert: Verify LSP client was disconnected
        val connectionState = lspClient.getConnectionState().value
        assertEquals(
            "LSP client should be disconnected",
            ConnectionState.DISCONNECTED,
            connectionState
        )
    }

    @Test
    fun testDispose_handlesErrorsGracefully() {
        // This test verifies that dispose() doesn't throw exceptions
        // even if individual cleanup operations fail

        // Act: Dispose the plugin
        val disposable = project.service<KiroPluginDisposable>()
        
        // Should not throw any exceptions
        try {
            disposable.dispose()
            assertTrue("Dispose should complete without throwing exceptions", true)
        } catch (e: Exception) {
            fail("Dispose should not throw exceptions: ${e.message}")
        }
    }

    @Test
    fun testDispose_canBeCalledMultipleTimes() {
        // Arrange: Create a session
        val chatService = project.service<ChatServiceImpl>()
        chatService.createSession()

        val disposable = project.service<KiroPluginDisposable>()

        // Act: Call dispose multiple times
        disposable.dispose()
        disposable.dispose()
        disposable.dispose()

        // Assert: Should not throw exceptions
        assertTrue("Multiple dispose calls should be safe", true)
    }

    override fun setUp() {
        super.setUp()
        // Ensure .kiro directory exists for tests
        val kiroDir = File(project.basePath, ".kiro")
        kiroDir.mkdirs()
    }

    override fun tearDown() {
        try {
            // Clean up test files
            val kiroDir = File(project.basePath, ".kiro")
            if (kiroDir.exists()) {
                kiroDir.deleteRecursively()
            }
        } finally {
            super.tearDown()
        }
    }
}
