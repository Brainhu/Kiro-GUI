package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.AuthState
import com.github.brainhu.kirogui.model.KiroCredentials
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.BrowserUtil
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * Application-level service implementing [AuthService].
 * Uses JetBrains PasswordSafe for secure credential storage and
 * BrowserUtil for opening the OAuth authorization URL.
 */
@Service(Service.Level.APP)
class AuthServiceImpl : AuthService {

    private val log = Logger.getInstance(AuthServiceImpl::class.java)

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)

    companion object {
        private const val SUBSYSTEM = "Kiro"
        private const val ACCESS_TOKEN_KEY = "access-token"
        private const val REFRESH_TOKEN_KEY = "refresh-token"
        private const val EXPIRES_AT_KEY = "expires-at"

        /** Default OAuth authorization URL (placeholder – configure per environment). */
        private const val OAUTH_URL = "https://auth.kiro.dev/authorize"

        /** Maximum number of retry attempts for token refresh. */
        private const val MAX_REFRESH_RETRIES = 3
    }

    init {
        // Restore auth state from stored credentials on startup
        restoreAuthState()
    }

    // ── Public API ──────────────────────────────────────────────────────

    override fun getAuthState(): StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun startOAuthFlow() {
        log.info("Starting OAuth flow – opening browser")
        try {
            BrowserUtil.browse(OAUTH_URL)
        } catch (e: Exception) {
            log.warn("Failed to open browser for OAuth", e)
        }
    }

    override suspend fun refreshToken(): Boolean {
        val credentials = getCredentials()
        if (credentials == null) {
            log.warn("Cannot refresh token – no stored credentials")
            _authState.value = AuthState.NotAuthenticated
            return false
        }

        _authState.value = AuthState.Refreshing

        for (attempt in 1..MAX_REFRESH_RETRIES) {
            try {
                val newCredentials = performTokenRefresh(credentials.refreshToken)
                if (newCredentials != null) {
                    storeCredentials(newCredentials)
                    _authState.value = AuthState.Authenticated(
                        username = "user",
                        email = "user@example.com"
                    )
                    log.info("Token refreshed successfully on attempt $attempt")
                    return true
                }
            } catch (e: Exception) {
                log.warn("Token refresh attempt $attempt failed", e)
            }

            if (attempt < MAX_REFRESH_RETRIES) {
                // Brief pause before retrying
                kotlinx.coroutines.delay(1000L * attempt)
            }
        }

        // All retries exhausted – clear credentials and reset state (Req 8.4)
        log.warn("Token refresh failed after $MAX_REFRESH_RETRIES attempts – logging out")
        logout()
        return false
    }

    override fun logout() {
        log.info("Logging out – clearing credentials")
        clearStoredCredentials()
        _authState.value = AuthState.NotAuthenticated
    }

    override fun getCredentials(): KiroCredentials? {
        return try {
            val accessToken = readSecret(ACCESS_TOKEN_KEY) ?: return null
            val refreshToken = readSecret(REFRESH_TOKEN_KEY) ?: return null
            val expiresAtStr = readSecret(EXPIRES_AT_KEY) ?: return null
            val expiresAt = Instant.parse(expiresAtStr)
            KiroCredentials(accessToken, refreshToken, expiresAt)
        } catch (e: Exception) {
            log.warn("Failed to read stored credentials", e)
            null
        }
    }

    // ── Credential storage helpers (PasswordSafe) ───────────────────────

    /**
     * Persist [KiroCredentials] into JetBrains PasswordSafe.
     */
    fun storeCredentials(credentials: KiroCredentials) {
        writeSecret(ACCESS_TOKEN_KEY, credentials.accessToken)
        writeSecret(REFRESH_TOKEN_KEY, credentials.refreshToken)
        writeSecret(EXPIRES_AT_KEY, credentials.expiresAt.toString())
    }

    /**
     * Called after a successful OAuth callback to save credentials and update state.
     */
    fun onOAuthSuccess(credentials: KiroCredentials, username: String, email: String) {
        storeCredentials(credentials)
        _authState.value = AuthState.Authenticated(username, email)
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private fun restoreAuthState() {
        val credentials = getCredentials() ?: return
        if (credentials.expiresAt.isAfter(Instant.now())) {
            _authState.value = AuthState.Authenticated(username = "user", email = "user@example.com")
        } else {
            _authState.value = AuthState.TokenExpired
        }
    }

    private fun clearStoredCredentials() {
        writeSecret(ACCESS_TOKEN_KEY, null)
        writeSecret(REFRESH_TOKEN_KEY, null)
        writeSecret(EXPIRES_AT_KEY, null)
    }

    /**
     * Simulate a token refresh call to the auth server.
     * In a real implementation this would make an HTTP request.
     */
    private fun performTokenRefresh(refreshToken: String): KiroCredentials? {
        // Placeholder – a real implementation would call the Kiro auth endpoint.
        // Return null to indicate failure; callers handle retry logic.
        return null
    }

    private fun credentialAttributes(key: String): CredentialAttributes {
        return CredentialAttributes(generateServiceName(SUBSYSTEM, key))
    }

    private fun writeSecret(key: String, value: String?) {
        val attrs = credentialAttributes(key)
        if (value != null) {
            PasswordSafe.instance.set(attrs, Credentials(key, value))
        } else {
            PasswordSafe.instance.set(attrs, null)
        }
    }

    private fun readSecret(key: String): String? {
        val attrs = credentialAttributes(key)
        return PasswordSafe.instance.get(attrs)?.getPasswordAsString()
    }
}
