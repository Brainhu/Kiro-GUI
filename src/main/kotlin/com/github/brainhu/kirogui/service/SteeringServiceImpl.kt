package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.SteeringRuleFile
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.io.File
import java.time.Instant

/**
 * Project-level implementation of [SteeringService].
 *
 * - Lists Markdown rule files from `.kiro/steering/` directory
 * - Reads and writes rule file content (Markdown format)
 * - Watches for file changes using IntelliJ VFS [BulkFileListener] and auto-reloads rules
 *
 * Requirements: 6.1, 6.2, 6.4
 */
@Service(Service.Level.PROJECT)
class SteeringServiceImpl(private val project: Project) : SteeringService {

    private val log = Logger.getInstance(SteeringServiceImpl::class.java)

    /** Cached rule files, reloaded on VFS changes. */
    internal var cachedRuleFiles: List<SteeringRuleFile>? = null

    private var listenerConnection: com.intellij.util.messages.MessageBusConnection? = null

    // ── Public API ──────────────────────────────────────────────────────

    override fun listRuleFiles(): List<SteeringRuleFile> {
        cachedRuleFiles?.let { return it }
        val loaded = loadRuleFilesFromDisk()
        cachedRuleFiles = loaded
        return loaded
    }

    override fun getRuleContent(fileName: String): String {
        val file = resolveRuleFile(fileName)
        if (!file.isFile) {
            throw IllegalArgumentException("Rule file not found: $fileName")
        }
        return file.readText()
    }

    override fun saveRuleContent(fileName: String, content: String) {
        val steeringDir = getOrCreateSteeringDir()
        val file = File(steeringDir, fileName)
        file.writeText(content)
        invalidateCache()
        log.info("Saved steering rule file: $fileName")
    }

    override fun createRuleFile(fileName: String): SteeringRuleFile {
        val steeringDir = getOrCreateSteeringDir()
        val file = File(steeringDir, fileName)
        if (file.exists()) {
            throw IllegalArgumentException("Rule file already exists: $fileName")
        }
        file.writeText("")
        invalidateCache()
        log.info("Created steering rule file: $fileName")
        return SteeringRuleFile(
            name = fileName,
            path = file.absolutePath,
            lastModified = Instant.ofEpochMilli(file.lastModified())
        )
    }

    override fun watchForChanges() {
        stopWatching()

        val connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                val steeringPath = getSteeringDir()?.absolutePath ?: return
                val relevant = events.any { event ->
                    isSteeringEvent(event, steeringPath)
                }
                if (relevant) {
                    invalidateCache()
                    log.info("Steering rule files changed, cache invalidated")
                }
            }
        })
        listenerConnection = connection
        log.info("Started watching for steering file changes")
    }

    override fun stopWatching() {
        listenerConnection?.disconnect()
        listenerConnection = null
    }

    // ── Internal helpers ────────────────────────────────────────────────

    internal fun invalidateCache() {
        cachedRuleFiles = null
    }

    private fun isSteeringEvent(event: VFileEvent, steeringPath: String): Boolean {
        val eventPath = event.path.replace('\\', '/')
        val normalizedSteeringPath = steeringPath.replace('\\', '/')
        if (!eventPath.startsWith(normalizedSteeringPath)) return false

        return when (event) {
            is VFileContentChangeEvent,
            is VFileCreateEvent,
            is VFileDeleteEvent -> true
            else -> false
        }
    }

    private fun loadRuleFilesFromDisk(): List<SteeringRuleFile> {
        val steeringDir = getSteeringDir() ?: return emptyList()
        if (!steeringDir.isDirectory) return emptyList()

        return steeringDir.listFiles()
            ?.filter { it.isFile && it.extension == "md" }
            ?.map { file ->
                SteeringRuleFile(
                    name = file.name,
                    path = file.absolutePath,
                    lastModified = Instant.ofEpochMilli(file.lastModified())
                )
            }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    private fun resolveRuleFile(fileName: String): File {
        val steeringDir = getSteeringDir()
            ?: throw IllegalArgumentException("Steering directory not found")
        return File(steeringDir, fileName)
    }

    private fun getSteeringDir(): File? {
        val basePath = project.basePath ?: return null
        return File(basePath, ".kiro/steering")
    }

    private fun getOrCreateSteeringDir(): File {
        val basePath = project.basePath
            ?: throw IllegalStateException("Project base path is not available")
        val dir = File(basePath, ".kiro/steering")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
