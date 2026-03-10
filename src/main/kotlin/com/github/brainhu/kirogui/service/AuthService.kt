package com.github.brainhu.kirogui.service

import com.github.brainhu.kirogui.model.AuthState
import com.github.brainhu.kirogui.model.KiroCredentials
import kotlinx.coroutines.flow.StateFlow

/**
 * Service interface for authentication and credential management.
 * Provides reactive auth state observation, OAuth flow, token refresh, and logout.
 */
interface AuthService {
    /**
     * Observe the current authentication state reactively.
     */
    fun getAuthState(): StateFlow<AuthState>

    /**
     * Start the OAuth authorization flow by opening the browser.
     */
    suspend fun startOAuthFlow()

    /**
     * Attempt to refresh the access token using the stored refresh token.
     * @return true if refresh succeeded, false otherwise.
     */
    suspend fun refreshToken(): Boolean

    /**
     * Log out: clear stored credentials and reset auth state to NotAuthenticated.
     */
    fun logout()

    /**
     * Retrieve the currently stored credentials, or null if not authenticated.
     */
    fun getCredentials(): KiroCredentials?
}
