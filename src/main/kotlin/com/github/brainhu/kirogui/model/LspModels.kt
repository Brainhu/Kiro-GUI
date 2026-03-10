package com.github.brainhu.kirogui.model

import kotlinx.serialization.Serializable

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FAILED
}

@Serializable
data class Position(
    val line: Int,
    val character: Int
)

@Serializable
data class ResponseMetadata(
    val requestId: String? = null,
    val model: String? = null,
    val tokensUsed: Int? = null
)

@Serializable
data class ChatRequest(
    val sessionId: String,
    val message: String,
    val context: MessageContext? = null
)

@Serializable
data class MessageContext(
    val filePath: String? = null,
    val selectedText: String? = null,
    val cursorPosition: Position? = null,
    val language: String? = null
)

@Serializable
data class ChatResponseChunk(
    val content: String,
    val isComplete: Boolean = false,
    val metadata: ResponseMetadata? = null
)
