package com.github.brainhu.kirogui

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Manages degraded mode for the Kiro plugin when core components fail to initialize.
 * Tracks which features are unavailable and displays appropriate notifications to users.
 *
 * Requirements: 1.3
 */
class DegradedModeManager(private val project: Project) {
    private val log = Logger.getInstance(DegradedModeManager::class.java)
    private val disabledFeatures = mutableSetOf<Feature>()

    /**
     * Enter degraded mode for a specific feature due to an initialization error.
     * Displays a warning notification to the user with error details.
     */
    fun enterDegradedMode(feature: Feature, error: Throwable) {
        disabledFeatures.add(feature)
        log.warn("Kiro feature '${feature.displayName}' entering degraded mode", error)
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Kiro")
            .createNotification(
                "Kiro: ${feature.displayName} 不可用",
                error.message ?: "未知错误",
                NotificationType.WARNING
            )
            .notify(project)
    }

    /**
     * Check if a specific feature is available (not in degraded mode).
     */
    fun isFeatureAvailable(feature: Feature): Boolean =
        feature !in disabledFeatures

    /**
     * Get all features currently in degraded mode.
     */
    fun getDisabledFeatures(): Set<Feature> = disabledFeatures.toSet()

    /**
     * Enumeration of plugin features that can enter degraded mode.
     */
    enum class Feature(val displayName: String) {
        CHAT("Chat 面板"),
        SPEC("Spec 管理"),
        HOOKS("Hook 自动化"),
        STEERING("Steering 配置"),
        LSP("LSP 通信")
    }
}
