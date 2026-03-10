package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.ChatRequest
import com.github.brainhu.kirogui.model.ChatResponseChunk
import com.github.brainhu.kirogui.model.ConnectionState
import com.github.brainhu.kirogui.model.ResponseMetadata
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future

/**
 * Project-level LSP client implementation using lsp4j.
 *
 * Manages the full connection lifecycle as a state machine:
 *   DISCONNECTED → CONNECTING → CONNECTED → DISCONNECTED
 *                                         → RECONNECTING → CONNECTED / FAILED
 *
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
@Service(Service.Level.PROJECT)
open class KiroLspClientImpl(private val project: Project) : KiroLspClient {

    private val log = Logger.getInstance(KiroLspClientImpl::class.java)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)

    /** The reconnection policy: max 3 retries, 5s delay. */
    internal val reconnectionPolicy = ReconnectionPolicy(maxRetries = 3, retryDelayMs = 5000)

    /** The lsp4j language server proxy (set after successful connection). */
    private var languageServer: LanguageServer? = null

    /** The lsp4j launcher listening future (for cleanup). */
    private var launcherListening: Future<Void>? = null

    /** Tracks in-progress request jobs by requestId for cancellation support. */
    private val activeRequests = ConcurrentHashMap<String, Job>()

    /** Input/output streams for the LSP server process. */
    private var serverInputStream: InputStream? = null
    private var serverOutputStream: OutputStream? = null

    // ── Public API ──────────────────────────────────────────────────────

    override fun getConnectionState(): StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun initialize(): InitializeResult {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            log.info("LSP client already connected")
            return InitializeResult(ServerCapabilities())
        }

        _connectionState.value = ConnectionState.CONNECTING
        log.info("Initializing LSP connection for project: ${project.name}")

        return try {
            val server = connectToServer()
            languageServer = server

            // Perform LSP initialize handshake with capability negotiation (Req 4.2)
            val initParams = buildInitializeParams()
            val result = server.initialize(initParams).get()

            // Notify the server that initialization is complete
            server.initialized(InitializedParams())

            _connectionState.value = ConnectionState.CONNECTED
            log.info("LSP connection established successfully")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("LSP initialization failed", e)
            _connectionState.value = ConnectionState.FAILED
            throw e
        }
    }

    override suspend fun sendMessage(request: ChatRequest): Flow<ChatResponseChunk> = callbackFlow {
        val server = languageServer
        if (server == null || _connectionState.value != ConnectionState.CONNECTED) {
            throw IllegalStateException("LSP client is not connected")
        }

        val requestId = request.sessionId + "-" + System.nanoTime()

        // Track this coroutine's job for cancellation support (Req 4.5)
        val job = Job()
        activeRequests[requestId] = job

        try {
            // Build LSP request parameters for the chat message
            val params = buildChatRequestParams(request)

            // Simulate streaming by sending the request and processing chunks (Req 4.6)
            // In a real implementation, this would use LSP's streaming/notification mechanism
            val responseFuture = server.textDocumentService
                ?.completion(params as CompletionParams)

            // For the actual Kiro LSP server, streaming would be handled via
            // custom notifications. Here we model the flow-based streaming interface.
            val chunk = ChatResponseChunk(
                content = "",
                isComplete = false,
                metadata = ResponseMetadata(requestId = requestId)
            )
            trySend(chunk)

            // Wait for the response (or cancellation)
            job.join()
        } catch (e: CancellationException) {
            log.info("Request $requestId was cancelled")
            // Send a final empty chunk to signal cancellation
            trySend(ChatResponseChunk(content = "", isComplete = true,
                metadata = ResponseMetadata(requestId = requestId)))
        } catch (e: Exception) {
            log.warn("Error during message send for request $requestId", e)
            close(e)
        } finally {
            activeRequests.remove(requestId)
        }

        awaitClose {
            activeRequests.remove(requestId)
            job.cancel()
        }
    }

    override suspend fun cancelRequest(requestId: String) {
        val job = activeRequests[requestId]
        if (job != null) {
            log.info("Cancelling request: $requestId")
            job.cancel()
            activeRequests.remove(requestId)
        } else {
            log.info("No active request found for id: $requestId")
        }
    }

    override suspend fun reconnect() {
        val currentState = _connectionState.value
        if (currentState == ConnectionState.CONNECTED) {
            log.info("Already connected, skipping reconnect")
            return
        }

        _connectionState.value = ConnectionState.RECONNECTING
        log.info("Attempting to reconnect to LSP server")

        // Cleanup any existing connection
        disconnectInternal()

        val success = reconnectionPolicy.executeWithRetry {
            val server = connectToServer()
            languageServer = server

            val initParams = buildInitializeParams()
            server.initialize(initParams).get()
            server.initialized(InitializedParams())
        }

        if (success) {
            _connectionState.value = ConnectionState.CONNECTED
            log.info("Reconnection successful")
        } else {
            _connectionState.value = ConnectionState.FAILED
            log.warn("Reconnection failed after ${reconnectionPolicy.maxRetries} attempts")
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────

    /**
     * Establish a connection to the Kiro LSP server process.
     * In production, this would launch or connect to the Kiro server process.
     */
    internal open fun connectToServer(): LanguageServer {
        // In a real implementation, this would:
        // 1. Find or launch the Kiro LSP server process
        // 2. Connect via stdin/stdout or socket
        // For now, we create a launcher with placeholder streams.
        val processBuilder = ProcessBuilder("kiro-lsp-server")
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        serverInputStream = process.inputStream
        serverOutputStream = process.outputStream

        val launcher: Launcher<LanguageServer> = LSPLauncher.createClientLauncher(
            KiroLanguageClientImpl(),
            serverInputStream!!,
            serverOutputStream!!
        )

        launcherListening = launcher.startListening()
        return launcher.remoteProxy
    }

    private fun buildInitializeParams(): InitializeParams {
        val params = InitializeParams()
        params.rootUri = project.basePath?.let { "file://$it" }
        params.processId = ProcessHandle.current().pid().toInt()

        // Declare client capabilities (Req 4.2)
        val capabilities = ClientCapabilities()
        val textDocCaps = TextDocumentClientCapabilities()
        capabilities.textDocument = textDocCaps

        val workspaceCaps = WorkspaceClientCapabilities()
        capabilities.workspace = workspaceCaps

        params.capabilities = capabilities
        return params
    }

    private fun buildChatRequestParams(request: ChatRequest): Any {
        // In a real Kiro LSP implementation, this would build custom request params.
        // Using CompletionParams as a placeholder for the LSP request structure.
        val params = CompletionParams()
        if (request.context?.filePath != null) {
            params.textDocument = TextDocumentIdentifier(request.context.filePath)
            params.position = org.eclipse.lsp4j.Position(
                request.context.cursorPosition?.line ?: 0,
                request.context.cursorPosition?.character ?: 0
            )
        } else {
            params.textDocument = TextDocumentIdentifier("file:///virtual")
            params.position = org.eclipse.lsp4j.Position(0, 0)
        }
        return params
    }

    /**
     * Clean up the current connection resources.
     */
    internal open fun disconnectInternal() {
        try {
            languageServer?.shutdown()?.get()
            languageServer?.exit()
        } catch (e: Exception) {
            log.warn("Error during LSP shutdown", e)
        }

        launcherListening?.cancel(true)
        launcherListening = null
        languageServer = null

        try {
            serverInputStream?.close()
            serverOutputStream?.close()
        } catch (e: Exception) {
            log.warn("Error closing LSP streams", e)
        }
        serverInputStream = null
        serverOutputStream = null
    }

    /**
     * Disconnect and transition to DISCONNECTED state.
     */
    fun disconnect() {
        disconnectInternal()
        activeRequests.values.forEach { it.cancel() }
        activeRequests.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
