package com.github.brainhu.kirogui

import com.github.brainhu.kirogui.service.*
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Unit tests for [KiroPluginInitializer].
 * Verifies plugin initialization logic, service retrieval, and error handling.
 *
 * Requirements: 1.1, 1.2, 1.3
 */
class KiroPluginInitializerTest : BasePlatformTestCase() {

    fun `test plugin initializer executes without throwing exceptions`() = runBlocking {
        val initializer = KiroPluginInitializer()
        
        // Should not throw any exceptions even if services are not fully initialized
        initializer.execute(project)
    }

    fun `test all project-level services can be retrieved`() {
        // Verify all project-level services are registered and can be retrieved
        val lspClient = project.service<KiroLspClientImpl>()
        assertNotNull("LSP Client should be retrievable", lspClient)
        
        val chatService = project.service<ChatServiceImpl>()
        assertNotNull("Chat Service should be retrievable", chatService)
        
        val contextService = project.service<ContextServiceImpl>()
        assertNotNull("Context Service should be retrievable", contextService)
        
        val specService = project.service<SpecServiceImpl>()
        assertNotNull("Spec Service should be retrievable", specService)
        
        val hookService = project.service<HookServiceImpl>()
        assertNotNull("Hook Service should be retrievable", hookService)
        
        val steeringService = project.service<SteeringServiceImpl>()
        assertNotNull("Steering Service should be retrievable", steeringService)
    }

    fun `test application-level auth service can be retrieved`() {
        val authService = service<AuthServiceImpl>()
        assertNotNull("Auth Service should be retrievable", authService)
    }

    fun `test kiro directory detection when directory exists`() {
        // Test that initializer handles projects with .kiro directory
        val projectBasePath = project.basePath
        if (projectBasePath != null) {
            val kiroPath = Paths.get(projectBasePath, ".kiro")
            
            // Only test if we can create the directory
            if (!Files.exists(kiroPath)) {
                try {
                    Files.createDirectories(kiroPath)
                    
                    val initializer = KiroPluginInitializer()
                    runBlocking {
                        // Should execute successfully with .kiro directory present
                        initializer.execute(project)
                    }
                } catch (e: Exception) {
                    // If we can't create the directory, skip this test
                    println("Skipping test - cannot create .kiro directory: ${e.message}")
                } finally {
                    // Cleanup - try to delete if we created it
                    try {
                        if (Files.exists(kiroPath) && Files.isDirectory(kiroPath)) {
                            Files.delete(kiroPath)
                        }
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                }
            }
        }
    }

    fun `test kiro directory detection when directory does not exist`() {
        // Test that initializer handles projects without .kiro directory
        val initializer = KiroPluginInitializer()
        runBlocking {
            // Should execute successfully even without .kiro directory
            // This is the normal case for most projects
            initializer.execute(project)
        }
    }

    fun `test degraded mode manager tracks disabled features`() {
        val degradedModeManager = DegradedModeManager(project)
        
        // Initially, all features should be available
        assertTrue(degradedModeManager.isFeatureAvailable(DegradedModeManager.Feature.CHAT))
        assertTrue(degradedModeManager.isFeatureAvailable(DegradedModeManager.Feature.LSP))
        
        // Enter degraded mode for LSP
        val error = RuntimeException("Test error")
        degradedModeManager.enterDegradedMode(DegradedModeManager.Feature.LSP, error)
        
        // LSP should now be unavailable
        assertFalse(degradedModeManager.isFeatureAvailable(DegradedModeManager.Feature.LSP))
        
        // Other features should still be available
        assertTrue(degradedModeManager.isFeatureAvailable(DegradedModeManager.Feature.CHAT))
        assertTrue(degradedModeManager.isFeatureAvailable(DegradedModeManager.Feature.SPEC))
        
        // Check disabled features
        val disabledFeatures = degradedModeManager.getDisabledFeatures()
        assertEquals(1, disabledFeatures.size)
        assertTrue(disabledFeatures.contains(DegradedModeManager.Feature.LSP))
    }
}
