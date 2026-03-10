package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Project-level implementation of [ChatService].
 *
 * - Manages multiple [Session] instances with create/get/close lifecycle
 * - Delegates sendMessage() to [KiroLspClient] and collects streaming chunks
 * - Supports cancelGeneration() to cancel in-progress LSP requests
 * - Persists session data to `.kiro/sessions/` as JSON on dispose
 *
 * Requirements: 2.2, 2.6, 1.4
 */
@Service(Service.Level.PROJECT)
open class ChatServiceImpl(private val project: Project) : ChatService {

    private val log = Logger.getInstance(ChatServiceImpl::class.java)

    private val sessions = ConcurrentHashMap<String, Session>()

    /** Maps sessionId → active LSP requestId for cancellation support. */
    private val activeRequestIds = ConcurrentHashMap<String, String>()

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Obtain the LSP client instance. Overridable for testing.
     */
    protected open fun getLspClient(): KiroLspClient {
        return project.getService(KiroLspClientImpl::class.java)
    }

    override fun createSession(): Session {
        val now = Instant.now()
        val session = Session(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            messages = mutableListOf(),
            createdAt = now,
            updatedAt = now
        )
        sessions[session.id] = session
        log.info("Created session: ${session.id}")
        return session
    }

    override fun getSession(sessionId: String): Session? = sessions[sessionId]

    override fun getAllSessions(): List<Session> = sessions.values.toList()

    override suspend fun sendMessage(
        sessionId: String,
        message: String,
        context: MessageContext?
    ): Flow<ChatResponseChunk> {
        val session = sessions[sessionId]
            ?: throw IllegalArgumentException("Session not found: $sessionId")

        // Append the user message to session history (Req 2.6 – context continuity)
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = message,
            timestamp = Instant.now(),
            context = context
        )
        session.messages.add(userMessage)
        session.updatedAt = Instant.now()

        // Build the LSP request
        val request = ChatRequest(
            sessionId = sessionId,
            message = message,
            context = context
        )

        val lspClient = getLspClient()
        val requestId = "$sessionId-${System.nanoTime()}"
        activeRequestIds[sessionId] = requestId

        // Delegate to LSP client and collect streaming chunks (Req 2.2)
        val responseBuilder = StringBuilder()

        return lspClient.sendMessage(request).onEach { chunk ->
            responseBuilder.append(chunk.content)

            if (chunk.isComplete) {
                // Append the complete assistant message to session history
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    content = responseBuilder.toString(),
                    timestamp = Instant.now()
                )
                session.messages.add(assistantMessage)
                session.updatedAt = Instant.now()
                activeRequestIds.remove(sessionId)
            }
        }
    }

    override suspend fun cancelGeneration(sessionId: String) {
        val requestId = activeRequestIds.remove(sessionId)
        if (requestId != null) {
            log.info("Cancelling generation for session: $sessionId (request: $requestId)")
            val lspClient = getLspClient()
            lspClient.cancelRequest(requestId)
        } else {
            log.info("No active generation to cancel for session: $sessionId")
        }
    }

    override fun closeSession(sessionId: String) {
        val removed = sessions.remove(sessionId)
        activeRequestIds.remove(sessionId)
        if (removed != null) {
            log.info("Closed session: $sessionId")
        }
    }

    // ── Persistence (Req 1.4) ───────────────────────────────────────────

    /**
     * Persist all active sessions to `.kiro/sessions/` as JSON files.
     * Called during plugin dispose to save session state.
     */
    fun persistSessions() {
        val basePath = project.basePath ?: return
        val sessionsDir = File(basePath, ".kiro/sessions")

        if (sessions.isEmpty()) {
            log.info("No sessions to persist")
            return
        }

        try {
            sessionsDir.mkdirs()
            for (session in sessions.values) {
                val file = File(sessionsDir, "${session.id}.json")
                val jsonStr = json.encodeToString(session)
                file.writeText(jsonStr)
            }
            log.info("Persisted ${sessions.size} session(s) to ${sessionsDir.path}")
        } catch (e: Exception) {
            log.warn("Failed to persist sessions", e)
        }
    }

    /**
     * Load previously persisted sessions from `.kiro/sessions/`.
     */
    fun loadSessions() {
        val basePath = project.basePath ?: return
        val sessionsDir = File(basePath, ".kiro/sessions")

        if (!sessionsDir.isDirectory) return

        try {
            val jsonFiles = sessionsDir.listFiles { f -> f.extension == "json" } ?: return
            for (file in jsonFiles) {
                val session = json.decodeFromString<Session>(file.readText())
                sessions[session.id] = session
            }
            log.info("Loaded ${jsonFiles.size} session(s) from ${sessionsDir.path}")
        } catch (e: Exception) {
            log.warn("Failed to load sessions", e)
        }
    }
}
