package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.ChatRequest
import com.github.brainhu.kirogui.model.ChatResponseChunk
import com.github.brainhu.kirogui.model.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.lsp4j.InitializeResult

/**
 * Client interface for communicating with the Kiro LSP server.
 * Manages connection lifecycle, message streaming, and request cancellation.
 *
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
interface KiroLspClient {
    /**
     * Initialize the LSP connection and perform capability negotiation.
     * Transitions state: DISCONNECTED → CONNECTING → CONNECTED (or FAILED).
     */
    suspend fun initialize(): InitializeResult

    /**
     * Send a chat message and receive a streaming response.
     * Each emitted [ChatResponseChunk] represents an incremental piece of the AI response.
     * The final chunk will have [ChatResponseChunk.isComplete] set to true.
     */
    suspend fun sendMessage(request: ChatRequest): Flow<ChatResponseChunk>

    /**
     * Cancel an in-progress request identified by [requestId].
     * After cancellation, no further response chunks should be emitted for that request.
     */
    suspend fun cancelRequest(requestId: String)

    /**
     * Observe the current LSP connection state reactively.
     */
    fun getConnectionState(): StateFlow<ConnectionState>

    /**
     * Attempt to reconnect to the LSP server using the configured reconnection policy.
     * Transitions state: FAILED/DISCONNECTED → RECONNECTING → CONNECTED (or FAILED).
     */
    suspend fun reconnect()
}
