package com.github.brainhu.kirogui.service

import com.intellij.openapi.diagnostic.Logger
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

/**
 * Local lsp4j [LanguageClient] implementation that receives notifications
 * and requests from the Kiro LSP server.
 */
class KiroLanguageClientImpl : LanguageClient {

    private val log = Logger.getInstance(KiroLanguageClientImpl::class.java)

    override fun telemetryEvent(obj: Any?) {
        log.debug("Telemetry event: $obj")
    }

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {
        log.debug("Diagnostics received for: ${diagnostics?.uri}")
    }

    override fun showMessage(params: MessageParams?) {
        log.info("Server message [${params?.type}]: ${params?.message}")
    }

    override fun showMessageRequest(params: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> {
        log.info("Server message request: ${params?.message}")
        return CompletableFuture.completedFuture(null)
    }

    override fun logMessage(params: MessageParams?) {
        log.info("Server log [${params?.type}]: ${params?.message}")
    }
}
