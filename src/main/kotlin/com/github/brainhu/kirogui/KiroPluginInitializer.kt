package com.github.brainhu.kirogui

import com.github.brainhu.kirogui.service.*
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Plugin initializer that runs on project startup.
 * Initializes all core services and registers components.
 *
 * Initialization sequence:
 * 1. Detect and validate `.kiro` directory in project root
 * 2. Initialize all services via IntelliJ service manager
 * 3. Initialize LSP client connection
 * 4. Register hook event listeners
 * 5. Start steering file watching
 * 6. Handle any initialization errors gracefully with notifications
 *
 * Requirements: 1.1, 1.2, 1.3
 */
class KiroPluginInitializer : ProjectActivity {
    private val log = Logger.getInstance(KiroPluginInitializer::class.java)

    override suspend fun execute(project: Project) {
        log.info("Kiro plugin initialization started for project: ${project.name}")
        
        val degradedModeManager = DegradedModeManager(project)
        
        // Step 1: Detect .kiro directory in project root
        val kiroConfigExists = detectKiroDirectory(project)
        if (kiroConfigExists) {
            log.info("Detected .kiro directory in project root")
        } else {
            log.info("No .kiro directory found - plugin will run with default configuration")
        }
        
        // Step 2: Initialize all services via service manager
        try {
            initializeServices(project, degradedModeManager)
        } catch (e: Exception) {
            log.error("Critical error during service initialization", e)
            showErrorNotification(
                project,
                "Kiro 插件初始化失败",
                "无法初始化核心服务: ${e.message}"
            )
            return
        }
        
        log.info("Kiro plugin initialization completed successfully")
    }

    /**
     * Detect if `.kiro` directory exists in project root.
     * Requirements: 1.2
     */
    private fun detectKiroDirectory(project: Project): Boolean {
        val projectBasePath = project.basePath ?: return false
        val kiroPath = Paths.get(projectBasePath, ".kiro")
        return Files.exists(kiroPath) && Files.isDirectory(kiroPath)
    }

    /**
     * Initialize all core services and handle errors gracefully.
     * Requirements: 1.1, 1.3
     */
    private suspend fun initializeServices(project: Project, degradedModeManager: DegradedModeManager) {
        // Initialize AuthService (application-level)
        try {
            val authService = service<AuthServiceImpl>()
            log.info("AuthService initialized successfully")
        } catch (e: Exception) {
            log.error("Failed to initialize AuthService", e)
            // Auth service failure is not critical - continue with degraded auth
        }
        
        // Initialize LSP Client
        try {
            val lspClient = project.service<KiroLspClientImpl>()
            log.info("LSP Client service retrieved")
            
            // Note: We don't call initialize() here because it requires a running LSP server.
            // The LSP client will be initialized on-demand when first used (e.g., when sending a message).
            // This allows the plugin to start even if the LSP server is not available.
            log.info("LSP Client will be initialized on first use")
        } catch (e: Exception) {
            log.error("Failed to retrieve LSP Client service", e)
            degradedModeManager.enterDegradedMode(DegradedModeManager.Feature.LSP, e)
        }
        
        // Initialize ChatService
        try {
            val chatService = project.service<ChatServiceImpl>()
            log.info("ChatService initialized successfully")
        } catch (e: Exception) {
            log.error("Failed to initialize ChatService", e)
            degradedModeManager.enterDegradedMode(DegradedModeManager.Feature.CHAT, e)
        }
        
        // Initialize ContextService
        try {
            val contextService = project.service<ContextServiceImpl>()
            log.info("ContextService initialized successfully")
        } catch (e: Exception) {
            log.error("Failed to initialize ContextService", e)
            // Context service failure affects chat but is not critical
        }
        
        // Initialize SpecService
        try {
            val specService = project.service<SpecServiceImpl>()
            log.info("SpecService initialized successfully")
        } catch (e: Exception) {
            log.error("Failed to initialize SpecService", e)
            degradedModeManager.enterDegradedMode(DegradedModeManager.Feature.SPEC, e)
        }
        
        // Initialize HookService and register event listeners
        try {
            val hookService = project.service<HookServiceImpl>()
            log.info("HookService retrieved")
            
            try {
                hookService.registerEventListeners()
                log.info("Hook event listeners registered successfully")
            } catch (e: Exception) {
                log.error("Failed to register hook event listeners", e)
                degradedModeManager.enterDegradedMode(DegradedModeManager.Feature.HOOKS, e)
            }
        } catch (e: Exception) {
            log.error("Failed to initialize HookService", e)
            degradedModeManager.enterDegradedMode(DegradedModeManager.Feature.HOOKS, e)
        }
        
        // Initialize SteeringService and start file watching
        try {
            val steeringService = project.service<SteeringServiceImpl>()
            log.info("SteeringService retrieved")
            
            try {
                steeringService.watchForChanges()
                log.info("Steering file watching started successfully")
            } catch (e: Exception) {
                log.error("Failed to start steering file watching", e)
                degradedModeManager.enterDegradedMode(DegradedModeManager.Feature.STEERING, e)
            }
        } catch (e: Exception) {
            log.error("Failed to initialize SteeringService", e)
            degradedModeManager.enterDegradedMode(DegradedModeManager.Feature.STEERING, e)
        }
    }

    /**
     * Show an error notification to the user.
     * Requirements: 1.3
     */
    private fun showErrorNotification(project: Project, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Kiro")
            .createNotification(
                title,
                content,
                NotificationType.ERROR
            )
            .notify(project)
    }
}
