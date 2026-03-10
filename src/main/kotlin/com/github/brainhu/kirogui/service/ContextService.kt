package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.MessageContext
import com.github.brainhu.kirogui.model.Position
import com.intellij.openapi.editor.Editor

/**
 * Service for collecting editor context information to attach to chat messages.
 *
 * Requirements: 7.2, 2.7
 */
interface ContextService {
    /** Collect all available context from the given [editor] into a [MessageContext]. */
    fun collectContext(editor: Editor): MessageContext

    /** Return the currently selected text in the [editor], or null if nothing is selected. */
    fun getSelectedText(editor: Editor): String?

    /** Return the file path of the document open in the [editor], or null if unavailable. */
    fun getCurrentFilePath(editor: Editor): String?

    /** Return the primary cursor position in the [editor], or null if unavailable. */
    fun getCursorPosition(editor: Editor): Position?

    /** Return the programming language of the file open in the [editor], or null if unavailable. */
    fun getFileLanguage(editor: Editor): String?
}
