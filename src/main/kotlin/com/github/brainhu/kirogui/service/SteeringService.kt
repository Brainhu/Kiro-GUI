package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.SteeringRuleFile

/**
 * Service for managing Steering rule files that customize AI assistant behavior
 * and project context.
 *
 * Reads, writes, and watches Markdown rule files in `.kiro/steering/` directory.
 * Automatically reloads rules when files change on disk.
 *
 * Requirements: 6.1, 6.2, 6.4
 */
interface SteeringService {
    /** List all rule files from `.kiro/steering/` directory. */
    fun listRuleFiles(): List<SteeringRuleFile>

    /** Read the Markdown content of a rule file by name. */
    fun getRuleContent(fileName: String): String

    /** Save Markdown content to a rule file, creating it if necessary. */
    fun saveRuleContent(fileName: String, content: String)

    /** Create a new empty rule file and return its metadata. */
    fun createRuleFile(fileName: String): SteeringRuleFile

    /** Start watching for file changes in the steering directory and auto-reload rules. */
    fun watchForChanges()

    /** Stop watching for file changes. */
    fun stopWatching()
}
