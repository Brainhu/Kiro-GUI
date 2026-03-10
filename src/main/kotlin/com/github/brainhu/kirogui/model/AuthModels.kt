package com.github.brainhu.kirogui.model

import java.time.Instant

sealed class AuthState {
    object NotAuthenticated : AuthState()
    data class Authenticated(val username: String, val email: String) : AuthState()
    object TokenExpired : AuthState()
    object Refreshing : AuthState()
}

data class KiroCredentials(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant
)
