package com.github.brainhu.kirogui.service

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReconnectionPolicyTest {

    @Test
    fun `executeWithRetry succeeds on first attempt`() = runBlocking {
        val policy = ReconnectionPolicy(maxRetries = 3, retryDelayMs = 0)
        var callCount = 0

        val result = policy.executeWithRetry {
            callCount++
        }

        assertTrue(result)
        assertEquals(1, callCount)
        assertEquals(0, policy.retryCount)
    }

    @Test
    fun `executeWithRetry succeeds after failures`() = runBlocking {
        val policy = ReconnectionPolicy(maxRetries = 3, retryDelayMs = 0)
        var callCount = 0

        val result = policy.executeWithRetry {
            callCount++
            if (callCount < 3) throw RuntimeException("Connection failed")
        }

        assertTrue(result)
        assertEquals(3, callCount)
        assertEquals(0, policy.retryCount) // reset on success
    }

    @Test
    fun `executeWithRetry fails after max retries exhausted`() = runBlocking {
        val policy = ReconnectionPolicy(maxRetries = 3, retryDelayMs = 0)
        var callCount = 0

        val result = policy.executeWithRetry {
            callCount++
            throw RuntimeException("Connection failed")
        }

        assertFalse(result)
        assertEquals(3, callCount)
        assertEquals(3, policy.retryCount)
    }

    @Test
    fun `reset clears retry count`() {
        val policy = ReconnectionPolicy(maxRetries = 3, retryDelayMs = 0)

        runBlocking {
            policy.executeWithRetry { throw RuntimeException("fail") }
        }
        assertEquals(3, policy.retryCount)

        policy.reset()
        assertEquals(0, policy.retryCount)
    }

    @Test
    fun `default policy has correct values`() {
        val policy = ReconnectionPolicy()
        assertEquals(3, policy.maxRetries)
        assertEquals(5000, policy.retryDelayMs)
    }
}
