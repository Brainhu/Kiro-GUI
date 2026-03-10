package com.github.brainhu.kirogui

import com.github.brainhu.kirogui.service.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Project-level disposable for Kiro plugin resource cleanup.
 *
 * Cleanup sequence on project close or plugin unload:
 * 1. Save all active ChatService sessions to `.kiro/sessions/` as JSON
 * 2. Unregister HookService event listeners
 * 3. Stop SteeringService file watching
 * 4. Disconnect LSP client
 *
 * Requirements: 1.4
 */
@Service(Service.Level.PROJECT)
class KiroPluginDisposable(private val project: Project) : Disposable {
    private val log = Logger.getInstance(KiroPluginDisposable::class.java)

    override fun dispose() {
        log.info("Kiro plugin cleanup started for project: ${project.name}")

        // Step 1: Save all active ChatService sessions
        try {
            val chatService = project.service<ChatServiceImpl>()
            chatService.persistSessions()
            log.info("ChatService sessions persisted successfully")
        } catch (e: Exception) {
            log.warn("Failed to persist ChatService sessions", e)
        }

        // Step 2: Unregister HookService event listeners
        try {
            val hookService = project.service<HookServiceImpl>()
            hookService.unregisterEventListeners()
            log.info("HookService event listeners unregistered successfully")
        } catch (e: Exception) {
            log.warn("Failed to unregister HookService event listeners", e)
        }

        // Step 3: Stop SteeringService file watching
        try {
            val steeringService = project.service<SteeringServiceImpl>()
            steeringService.stopWatching()
            log.info("SteeringService file watching stopped successfully")
        } catch (e: Exception) {
            log.warn("Failed to stop SteeringService file watching", e)
        }

        // Step 4: Disconnect LSP client
        try {
            val lspClient = project.service<KiroLspClientImpl>()
            lspClient.disconnect()
            log.info("LSP client disconnected successfully")
        } catch (e: Exception) {
            log.warn("Failed to disconnect LSP client", e)
        }

        log.info("Kiro plugin cleanup completed for project: ${project.name}")
    }
}
