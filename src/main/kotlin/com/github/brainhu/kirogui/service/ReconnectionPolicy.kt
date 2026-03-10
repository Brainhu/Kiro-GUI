package com.github.brainhu.kirogui.service

import kotlinx.coroutines.delay

/**
 * Encapsulates the retry logic for LSP reconnection attempts.
 * Default policy: max 3 retries with 5-second delay between attempts.
 *
 * Requirements: 4.3
 */
class ReconnectionPolicy(
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 5000
) {
    var retryCount: Int = 0
        private set

    /**
     * Execute [connect] with retry logic. Returns true if connection succeeded,
     * false if all retries were exhausted.
     */
    suspend fun executeWithRetry(connect: suspend () -> Unit): Boolean {
        retryCount = 0
        while (retryCount < maxRetries) {
            try {
                connect()
                retryCount = 0
                return true
            } catch (e: Exception) {
                retryCount++
                if (retryCount < maxRetries) {
                    delay(retryDelayMs)
                }
            }
        }
        return false
    }

    /**
     * Reset the retry counter (e.g., after a successful manual reconnect).
     */
    fun reset() {
        retryCount = 0
    }
}
