package com.github.brainhu.kirogui.ui

import com.github.brainhu.kirogui.model.AuthState
import com.github.brainhu.kirogui.model.ConnectionState
import com.github.brainhu.kirogui.service.AuthService
import com.github.brainhu.kirogui.service.KiroLspClient
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.Icon

/**
 * Factory for the Kiro status bar widget.
 * Displays connection state and auth status.
 */
class KiroStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "KiroStatusBarWidget"

    override fun getDisplayName(): String = "Kiro Status"

    override fun createWidget(project: Project): StatusBarWidget {
        return KiroStatusBarWidget(project)
    }

    override fun isAvailable(project: Project): Boolean = true
}

/**
 * Status bar widget showing Kiro connection and auth state.
 * 
 * Displays:
 * - Connection state with colored icons (connected/connecting/disconnected/failed)
 * - Auth state (username or "not logged in" prompt)
 * - Clickable actions for reconnect (when FAILED) and login (when not authenticated)
 * 
 * Requirements: 4.4, 8.5
 */
class KiroStatusBarWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.MultipleTextValuesPresentation {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lspClient = project.service<KiroLspClient>()
    private val authService = ApplicationManager.getApplication().service<AuthService>()
    
    private var currentConnectionState = ConnectionState.DISCONNECTED
    private var currentAuthState: AuthState = AuthState.NotAuthenticated
    
    init {
        // Observe connection and auth states reactively
        scope.launch {
            combine(
                lspClient.getConnectionState(),
                authService.getAuthState()
            ) { connState, authState ->
                Pair(connState, authState)
            }.collect { (connState, authState) ->
                currentConnectionState = connState
                currentAuthState = authState
                // Update the status bar display
                myStatusBar?.updateWidget(ID())
            }
        }
    }

    override fun ID(): String = "KiroStatusBarWidget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText(): String {
        val connText = when (currentConnectionState) {
            ConnectionState.CONNECTED -> "Connected"
            ConnectionState.CONNECTING -> "Connecting..."
            ConnectionState.DISCONNECTED -> "Disconnected"
            ConnectionState.RECONNECTING -> "Reconnecting..."
            ConnectionState.FAILED -> "Connection failed"
        }
        
        val authText = when (val auth = currentAuthState) {
            is AuthState.Authenticated -> "Logged in as ${auth.username}"
            AuthState.NotAuthenticated -> "Not logged in"
            AuthState.TokenExpired -> "Token expired"
            AuthState.Refreshing -> "Refreshing token..."
        }
        
        return "Kiro: $connText | $authText"
    }

    override fun getSelectedValue(): String {
        val connIcon = when (currentConnectionState) {
            ConnectionState.CONNECTED -> "●"
            ConnectionState.CONNECTING -> "○"
            ConnectionState.DISCONNECTED -> "○"
            ConnectionState.RECONNECTING -> "◐"
            ConnectionState.FAILED -> "✕"
        }
        
        val authText = when (val auth = currentAuthState) {
            is AuthState.Authenticated -> auth.username
            AuthState.NotAuthenticated -> "not logged in"
            AuthState.TokenExpired -> "token expired"
            AuthState.Refreshing -> "refreshing..."
        }
        
        return "Kiro $connIcon $authText"
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer { event ->
            showPopupMenu(event.component, event.x, event.y)
        }
    }

    private fun showPopupMenu(component: Component, x: Int, y: Int) {
        val actions = mutableListOf<PopupAction>()
        
        // Add reconnect action if connection is FAILED
        if (currentConnectionState == ConnectionState.FAILED) {
            actions.add(PopupAction("Reconnect to Kiro Server") {
                scope.launch {
                    lspClient.reconnect()
                }
            })
        }
        
        // Add login action if not authenticated
        if (currentAuthState is AuthState.NotAuthenticated || currentAuthState is AuthState.TokenExpired) {
            actions.add(PopupAction("Login to Kiro") {
                scope.launch {
                    authService.startOAuthFlow()
                }
            })
        }
        
        // Add logout action if authenticated
        if (currentAuthState is AuthState.Authenticated) {
            actions.add(PopupAction("Logout") {
                authService.logout()
            })
        }
        
        // Show status info
        actions.add(PopupAction("Connection: ${currentConnectionState.name}", null))
        actions.add(PopupAction("Auth: ${getAuthStateText()}", null))
        
        if (actions.isEmpty()) {
            return
        }
        
        val popup = createPopup(actions)
        popup.show(RelativePoint(component, java.awt.Point(x, y)))
    }

    private fun getAuthStateText(): String {
        return when (val auth = currentAuthState) {
            is AuthState.Authenticated -> "Authenticated (${auth.username})"
            AuthState.NotAuthenticated -> "Not Authenticated"
            AuthState.TokenExpired -> "Token Expired"
            AuthState.Refreshing -> "Refreshing..."
        }
    }

    private fun createPopup(actions: List<PopupAction>): ListPopup {
        return JBPopupFactory.getInstance().createListPopup(
            object : com.intellij.openapi.ui.popup.util.BaseListPopupStep<PopupAction>("Kiro Status", actions) {
                override fun getTextFor(value: PopupAction): String = value.text
                
                override fun onChosen(selectedValue: PopupAction, finalChoice: Boolean): com.intellij.openapi.ui.popup.PopupStep<*>? {
                    selectedValue.action?.invoke()
                    return com.intellij.openapi.ui.popup.PopupStep.FINAL_CHOICE
                }
                
                override fun isSelectable(value: PopupAction): Boolean = value.action != null
            }
        )
    }

    override fun dispose() {
        scope.cancel()
        super.dispose()
    }
    
    private data class PopupAction(val text: String, val action: (() -> Unit)?)
}
