package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.MessageContext
import com.github.brainhu.kirogui.model.Position
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

/**
 * Project-level implementation of [ContextService].
 *
 * Collects file path, selected text, cursor position, and language
 * from an [Editor] instance using IntelliJ Platform APIs.
 *
 * Requirements: 7.2, 2.7
 */
@Service(Service.Level.PROJECT)
class ContextServiceImpl(private val project: Project) : ContextService {

    private val log = Logger.getInstance(ContextServiceImpl::class.java)

    override fun collectContext(editor: Editor): MessageContext {
        return MessageContext(
            filePath = getCurrentFilePath(editor),
            selectedText = getSelectedText(editor),
            cursorPosition = getCursorPosition(editor),
            language = getFileLanguage(editor)
        )
    }

    override fun getSelectedText(editor: Editor): String? {
        val text = editor.selectionModel.selectedText
        return if (text.isNullOrEmpty()) null else text
    }

    override fun getCurrentFilePath(editor: Editor): String? {
        return try {
            val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
            virtualFile?.path
        } catch (e: Exception) {
            log.warn("Failed to get file path from editor", e)
            null
        }
    }

    override fun getCursorPosition(editor: Editor): Position? {
        return try {
            val offset = editor.caretModel.offset
            val logicalPosition = editor.offsetToLogicalPosition(offset)
            Position(line = logicalPosition.line, character = logicalPosition.column)
        } catch (e: Exception) {
            log.warn("Failed to get cursor position from editor", e)
            null
        }
    }

    override fun getFileLanguage(editor: Editor): String? {
        return try {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            psiFile?.language?.id
        } catch (e: Exception) {
            log.warn("Failed to get file language from editor", e)
            null
        }
    }
}
