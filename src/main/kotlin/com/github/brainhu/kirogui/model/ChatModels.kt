package com.github.brainhu.kirogui.model

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Session(
    val id: String,
    val title: String,
    val messages: MutableList<ChatMessage>,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    var updatedAt: Instant
)

@Serializable
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant,
    val context: MessageContext? = null
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}
