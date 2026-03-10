package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.SpecDetail
import com.github.brainhu.kirogui.model.SpecPhase
import com.github.brainhu.kirogui.model.SpecSummary

/**
 * Service for managing Spec-driven development workflows.
 *
 * Provides CRUD operations for specs stored under `.kiro/specs/` and
 * task status management within `tasks.md` files.
 *
 * Requirements: 3.1, 3.2, 3.3, 3.5, 3.6
 */
interface SpecService {
    /** List all specs by scanning the `.kiro/specs/` directory. */
    fun listSpecs(): List<SpecSummary>

    /** Get full spec detail including requirements, design, and parsed tasks. */
    fun getSpec(specName: String): SpecDetail?

    /** Create a new spec with directory structure and template files. */
    fun createSpec(name: String): SpecDetail

    /** Toggle a task's checkbox state in the spec's `tasks.md` file. */
    fun updateTaskStatus(specName: String, taskId: String, completed: Boolean)

    /** Determine the current phase of a spec based on document existence and task completion. */
    fun getSpecPhase(specName: String): SpecPhase
}
