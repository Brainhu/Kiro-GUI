package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.HookConfig
import com.github.brainhu.kirogui.model.HookExecutionRecord

/**
 * Service for managing Hook automation workflows.
 *
 * Reads hook configurations from `.kiro/hooks/`, registers IDE event listeners
 * for enabled hooks, executes hooks on matching events, and records execution results.
 *
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
interface HookService {
    /** List all hook configurations from `.kiro/hooks/` directory. */
    fun listHooks(): List<HookConfig>

    /** Enable or disable a hook by ID, persisting the change to its config file. */
    fun setHookEnabled(hookId: String, enabled: Boolean)

    /** Get the execution log of all hook runs. */
    fun getExecutionLog(): List<HookExecutionRecord>

    /** Register IDE event listeners for all enabled hooks. */
    fun registerEventListeners()

    /** Unregister all IDE event listeners. */
    fun unregisterEventListeners()
}
