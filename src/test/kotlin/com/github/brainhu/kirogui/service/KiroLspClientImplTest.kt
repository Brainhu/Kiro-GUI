package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.ChatRequest
import com.github.brainhu.kirogui.model.ChatResponseChunk
import com.github.brainhu.kirogui.model.ConnectionState
import com.github.brainhu.kirogui.model.ResponseMetadata
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Unit tests for KiroLspClient state machine and core behavior.
 * Uses [TestableKiroLspClient] to avoid needing a real IntelliJ Project.
 */
class KiroLspClientImplTest {

    @Test
    fun `initial state is DISCONNECTED`() {
        val client = TestableKiroLspClient(FakeLanguageServer())
        assertEquals(ConnectionState.DISCONNECTED, client.getConnectionState().value)
    }

    @Test
    fun `initialize transitions to CONNECTED on success`() = runBlocking {
        val server = FakeLanguageServer()
        val client = TestableKiroLspClient(server)

        val result = client.initialize()

        assertEquals(ConnectionState.CONNECTED, client.getConnectionState().value)
        assertTrue(server.initializeCalled)
        assertTrue(server.initializedCalled)
        assertNotNull(result)
    }

    @Test
    fun `initialize transitions to FAILED on error`() {
        val server = FakeLanguageServer().apply { shouldFailInitialize = true }
        val client = TestableKiroLspClient(server)

        assertThrows(Exception::class.java) {
            runBlocking { client.initialize() }
        }
        assertEquals(ConnectionState.FAILED, client.getConnectionState().value)
    }

    @Test
    fun `initialize is idempotent when already connected`() = runBlocking {
        val client = TestableKiroLspClient(FakeLanguageServer())
        client.initialize()
        assertEquals(ConnectionState.CONNECTED, client.getConnectionState().value)

        val result = client.initialize()
        assertEquals(ConnectionState.CONNECTED, client.getConnectionState().value)
        assertNotNull(result)
    }

    @Test
    fun `disconnect transitions to DISCONNECTED`() = runBlocking {
        val client = TestableKiroLspClient(FakeLanguageServer())
        client.initialize()
        assertEquals(ConnectionState.CONNECTED, client.getConnectionState().value)

        client.disconnect()
        assertEquals(ConnectionState.DISCONNECTED, client.getConnectionState().value)
    }

    @Test
    fun `cancelRequest on non-existent id does not throw`() = runBlocking {
        val client = TestableKiroLspClient(FakeLanguageServer())
        client.cancelRequest("non-existent-id")
    }

    @Test
    fun `sendMessage throws when not connected`() {
        val client = TestableKiroLspClient(FakeLanguageServer())
        val request = ChatRequest(sessionId = "s1", message = "hello")

        assertThrows(IllegalStateException::class.java) {
            runBlocking { client.sendMessage(request).first() }
        }
    }

    @Test
    fun `reconnect transitions to CONNECTED on success`() = runBlocking {
        var attempt = 0
        val client = TestableKiroLspClient {
            attempt++
            if (attempt == 1) throw RuntimeException("First attempt fails")
            FakeLanguageServer()
        }

        client.reconnect()
        assertEquals(ConnectionState.CONNECTED, client.getConnectionState().value)
    }

    @Test
    fun `reconnect transitions to FAILED after max retries`() = runBlocking {
        val client = TestableKiroLspClient {
            throw RuntimeException("Server unavailable")
        }

        client.reconnect()
        assertEquals(ConnectionState.FAILED, client.getConnectionState().value)
    }

    @Test
    fun `reconnect skips when already connected`() = runBlocking {
        val client = TestableKiroLspClient(FakeLanguageServer())
        client.initialize()

        client.reconnect()
        assertEquals(ConnectionState.CONNECTED, client.getConnectionState().value)
    }
}


/**
 * Testable LSP client that mirrors KiroLspClientImpl logic without
 * requiring a real IntelliJ Project instance.
 */
class TestableKiroLspClient private constructor(
    private val serverFactory: () -> LanguageServer
) : KiroLspClient {

    constructor(fakeServer: LanguageServer) : this({ fakeServer })

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val reconnectionPolicy = ReconnectionPolicy(maxRetries = 3, retryDelayMs = 0)
    private var languageServer: LanguageServer? = null
    private val activeRequests = ConcurrentHashMap<String, Job>()

    override fun getConnectionState(): StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun initialize(): InitializeResult {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            return InitializeResult(ServerCapabilities())
        }
        _connectionState.value = ConnectionState.CONNECTING
        return try {
            val server = serverFactory()
            languageServer = server
            val params = InitializeParams().apply {
                rootUri = "file:///test"
                processId = 0
                capabilities = ClientCapabilities()
            }
            val result = server.initialize(params).get()
            server.initialized(InitializedParams())
            _connectionState.value = ConnectionState.CONNECTED
            result
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.FAILED
            throw e
        }
    }

    override suspend fun sendMessage(request: ChatRequest): Flow<ChatResponseChunk> {
        if (languageServer == null || _connectionState.value != ConnectionState.CONNECTED) {
            throw IllegalStateException("LSP client is not connected")
        }
        return callbackFlow {
            val requestId = request.sessionId + "-" + System.nanoTime()
            val job = Job()
            activeRequests[requestId] = job
            try {
                trySend(ChatResponseChunk(
                    content = "",
                    isComplete = false,
                    metadata = ResponseMetadata(requestId = requestId)
                ))
                job.join()
            } finally {
                activeRequests.remove(requestId)
            }
            awaitClose { activeRequests.remove(requestId) }
        }
    }

    override suspend fun cancelRequest(requestId: String) {
        activeRequests[requestId]?.cancel()
        activeRequests.remove(requestId)
    }

    override suspend fun reconnect() {
        if (_connectionState.value == ConnectionState.CONNECTED) return
        _connectionState.value = ConnectionState.RECONNECTING
        val success = reconnectionPolicy.executeWithRetry {
            val server = serverFactory()
            languageServer = server
            val params = InitializeParams().apply {
                rootUri = "file:///test"
                processId = 0
                capabilities = ClientCapabilities()
            }
            server.initialize(params).get()
            server.initialized(InitializedParams())
        }
        _connectionState.value = if (success) ConnectionState.CONNECTED else ConnectionState.FAILED
    }

    fun disconnect() {
        try {
            languageServer?.shutdown()?.get()
            languageServer?.exit()
        } catch (_: Exception) {}
        languageServer = null
        activeRequests.values.forEach { it.cancel() }
        activeRequests.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}

/**
 * A simple fake LanguageServer for testing.
 */
class FakeLanguageServer : LanguageServer {
    var initializeCalled = false
    var initializedCalled = false
    var shutdownCalled = false
    var exitCalled = false
    var shouldFailInitialize = false

    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
        initializeCalled = true
        if (shouldFailInitialize) {
            return CompletableFuture.failedFuture(RuntimeException("Init failed"))
        }
        return CompletableFuture.completedFuture(InitializeResult(ServerCapabilities()))
    }

    override fun initialized(params: InitializedParams?) {
        initializedCalled = true
    }

    override fun shutdown(): CompletableFuture<Any> {
        shutdownCalled = true
        return CompletableFuture.completedFuture(null)
    }

    override fun exit() {
        exitCalled = true
    }

    override fun getTextDocumentService(): TextDocumentService? = null
    override fun getWorkspaceService(): WorkspaceService? = null
}
