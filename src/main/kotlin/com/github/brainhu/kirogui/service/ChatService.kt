package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.ChatResponseChunk
import com.github.brainhu.kirogui.model.MessageContext
import com.github.brainhu.kirogui.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Service for managing chat sessions and delegating messages to the LSP backend.
 *
 * Requirements: 2.2, 2.6, 1.4
 */
interface ChatService {
    /** Create a new chat session and return it. */
    fun createSession(): Session

    /** Retrieve an existing session by its ID, or null if not found. */
    fun getSession(sessionId: String): Session?

    /** Return all active sessions. */
    fun getAllSessions(): List<Session>

    /**
     * Send a user message within a session and receive a streaming AI response.
     * The message is appended to the session history before the LSP request is made.
     */
    suspend fun sendMessage(sessionId: String, message: String, context: MessageContext?): Flow<ChatResponseChunk>

    /** Cancel an in-progress AI generation for the given session. */
    suspend fun cancelGeneration(sessionId: String)

    /** Close and remove a session from the active set. */
    fun closeSession(sessionId: String)
}
