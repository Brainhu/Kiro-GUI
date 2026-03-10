package com.github.brainhu.kirogui.model

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class HookConfig(
    val id: String,
    val name: String,
    val trigger: HookTrigger,
    val enabled: Boolean,
    val filePath: String
)

enum class HookTrigger {
    FILE_SAVE, PRE_COMMIT, FILE_CREATE, FILE_DELETE
}

data class HookExecutionRecord(
    val hookId: String,
    val timestamp: Instant,
    val success: Boolean,
    val message: String,
    val triggerFile: String? = null
)
