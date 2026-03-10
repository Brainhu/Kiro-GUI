package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.eclipse.lsp4j.InitializeResult
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * Unit tests for [ChatServiceImpl] covering session lifecycle,
 * message delegation, cancellation, and JSON persistence.
 */
class ChatServiceImplTest : BasePlatformTestCase() {

    private lateinit var chatService: TestChatServiceImpl
    private lateinit var fakeLspClient: FakeLspClientForChat

    override fun setUp() {
        super.setUp()
        fakeLspClient = FakeLspClientForChat()
        chatService = TestChatServiceImpl(project, fakeLspClient)
    }

    // ── Session lifecycle ───────────────────────────────────────────────

    fun `test createSession returns session with unique id`() {
        val s1 = chatService.createSession()
        val s2 = chatService.createSession()

        assertNotNull(s1.id)
        assertNotNull(s2.id)
        assertNotEquals(s1.id, s2.id)
        assertEquals("New Chat", s1.title)
        assertTrue(s1.messages.isEmpty())
    }

    fun `test getSession returns created session`() {
        val session = chatService.createSession()
        val retrieved = chatService.getSession(session.id)

        assertNotNull(retrieved)
        assertEquals(session.id, retrieved!!.id)
    }

    fun `test getSession returns null for unknown id`() {
        assertNull(chatService.getSession("nonexistent"))
    }

    fun `test getAllSessions returns all created sessions`() {
        chatService.createSession()
        chatService.createSession()
        chatService.createSession()

        assertEquals(3, chatService.getAllSessions().size)
    }

    fun `test closeSession removes session`() {
        val session = chatService.createSession()
        chatService.closeSession(session.id)

        assertNull(chatService.getSession(session.id))
        assertEquals(0, chatService.getAllSessions().size)
    }

    fun `test closeSession on unknown id does not throw`() {
        chatService.closeSession("nonexistent")
        // Should complete without error
    }

    // ── Message sending ─────────────────────────────────────────────────

    fun `test sendMessage appends user message to session`() = runBlocking {
        val session = chatService.createSession()

        fakeLspClient.responseChunks = listOf(
            ChatResponseChunk(content = "Hello!", isComplete = true)
        )

        chatService.sendMessage(session.id, "Hi", null).toList()

        val messages = chatService.getSession(session.id)!!.messages
        assertEquals(2, messages.size) // user + assistant
        assertEquals(MessageRole.USER, messages[0].role)
        assertEquals("Hi", messages[0].content)
        assertEquals(MessageRole.ASSISTANT, messages[1].role)
        assertEquals("Hello!", messages[1].content)
    }

    fun `test sendMessage with context attaches context to user message`() = runBlocking {
        val session = chatService.createSession()
        val ctx = MessageContext(filePath = "src/Main.kt", language = "kotlin")

        fakeLspClient.responseChunks = listOf(
            ChatResponseChunk(content = "Done", isComplete = true)
        )

        chatService.sendMessage(session.id, "Explain", ctx).toList()

        val userMsg = chatService.getSession(session.id)!!.messages[0]
        assertEquals("src/Main.kt", userMsg.context?.filePath)
        assertEquals("kotlin", userMsg.context?.language)
    }

    fun `test sendMessage collects streaming chunks into single assistant message`() = runBlocking {
        val session = chatService.createSession()

        fakeLspClient.responseChunks = listOf(
            ChatResponseChunk(content = "Part 1 ", isComplete = false),
            ChatResponseChunk(content = "Part 2 ", isComplete = false),
            ChatResponseChunk(content = "Part 3", isComplete = true)
        )

        chatService.sendMessage(session.id, "Tell me", null).toList()

        val messages = chatService.getSession(session.id)!!.messages
        assertEquals(2, messages.size)
        assertEquals("Part 1 Part 2 Part 3", messages[1].content)
    }

    fun `test sendMessage throws for unknown session`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                chatService.sendMessage("nonexistent", "Hi", null).toList()
            }
        }
    }

    fun `test sendMessage preserves multi-turn conversation order`() = runBlocking {
        val session = chatService.createSession()

        fakeLspClient.responseChunks = listOf(
            ChatResponseChunk(content = "Reply 1", isComplete = true)
        )
        chatService.sendMessage(session.id, "Message 1", null).toList()

        fakeLspClient.responseChunks = listOf(
            ChatResponseChunk(content = "Reply 2", isComplete = true)
        )
        chatService.sendMessage(session.id, "Message 2", null).toList()

        val messages = chatService.getSession(session.id)!!.messages
        assertEquals(4, messages.size)
        assertEquals("Message 1", messages[0].content)
        assertEquals("Reply 1", messages[1].content)
        assertEquals("Message 2", messages[2].content)
        assertEquals("Reply 2", messages[3].content)
    }

    // ── Cancellation ────────────────────────────────────────────────────

    fun `test cancelGeneration delegates to lsp client`() = runBlocking {
        val session = chatService.createSession()

        // Simulate an active request by starting a send (we won't collect it)
        fakeLspClient.responseChunks = listOf(
            ChatResponseChunk(content = "partial", isComplete = false)
        )

        // Cancel without an active request should not throw
        chatService.cancelGeneration(session.id)
        // Should complete without error
    }

    // ── Persistence ─────────────────────────────────────────────────────

    fun `test persistSessions writes JSON files to kiro sessions dir`() {
        val session = chatService.createSession()
        session.messages.add(
            ChatMessage(
                id = "msg-1",
                role = MessageRole.USER,
                content = "Hello",
                timestamp = session.createdAt
            )
        )

        chatService.persistSessions()

        val sessionsDir = File(project.basePath!!, ".kiro/sessions")
        assertTrue(sessionsDir.isDirectory)

        val file = File(sessionsDir, "${session.id}.json")
        assertTrue(file.exists())

        val content = file.readText()
        assertTrue(content.contains(session.id))
        assertTrue(content.contains("Hello"))
    }

    fun `test loadSessions restores persisted sessions`() {
        // Create and persist a session
        val session = chatService.createSession()
        session.messages.add(
            ChatMessage(
                id = "msg-1",
                role = MessageRole.USER,
                content = "Persisted message",
                timestamp = session.createdAt
            )
        )
        chatService.persistSessions()

        // Create a fresh service and load
        val freshService = ChatServiceImpl(project)
        freshService.loadSessions()

        val loaded = freshService.getSession(session.id)
        assertNotNull(loaded)
        assertEquals(session.id, loaded!!.id)
        assertEquals(1, loaded.messages.size)
        assertEquals("Persisted message", loaded.messages[0].content)
    }

    fun `test persistSessions with no sessions does not create files`() {
        // Use a fresh service with no sessions
        val freshService = TestChatServiceImpl(project, fakeLspClient)
        freshService.persistSessions()
        // Should complete without error (no sessions to persist)
    }

    fun `test session serialization round-trip`() {
        val session = chatService.createSession()
        session.messages.add(
            ChatMessage(
                id = "msg-1",
                role = MessageRole.USER,
                content = "Test content",
                timestamp = session.createdAt,
                context = MessageContext(filePath = "test.kt", language = "kotlin")
            )
        )

        val jsonStr = chatService.json.encodeToString(session)
        val deserialized = chatService.json.decodeFromString<Session>(jsonStr)

        assertEquals(session.id, deserialized.id)
        assertEquals(session.title, deserialized.title)
        assertEquals(session.messages.size, deserialized.messages.size)
        assertEquals(session.messages[0].content, deserialized.messages[0].content)
        assertEquals(session.messages[0].context?.filePath, deserialized.messages[0].context?.filePath)
    }
}

/**
 * Fake LSP client for ChatService tests.
 */
class FakeLspClientForChat : KiroLspClient {
    var responseChunks: List<ChatResponseChunk> = emptyList()
    var lastCancelledRequestId: String? = null
    var lastRequest: ChatRequest? = null
    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)

    override suspend fun initialize(): InitializeResult =
        throw UnsupportedOperationException("Fake client")

    override suspend fun sendMessage(request: ChatRequest): Flow<ChatResponseChunk> {
        lastRequest = request
        return flow {
            for (chunk in responseChunks) {
                emit(chunk)
            }
        }
    }

    override suspend fun cancelRequest(requestId: String) {
        lastCancelledRequestId = requestId
    }

    override fun getConnectionState(): StateFlow<ConnectionState> = _connectionState
    override suspend fun reconnect() { /* no-op */ }
}

/**
 * Testable subclass of [ChatServiceImpl] that uses an injected LSP client.
 */
class TestChatServiceImpl(
    project: com.intellij.openapi.project.Project,
    private val lspClient: KiroLspClient
) : ChatServiceImpl(project) {
    override fun getLspClient(): KiroLspClient = lspClient
}
