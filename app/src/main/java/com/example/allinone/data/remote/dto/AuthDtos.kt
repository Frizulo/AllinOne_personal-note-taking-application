package com.example.allinone.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequest(
    val name: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val uid: Long,
    val name: String
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    val uid: Long
)

data class ApiErrorResponse(
    val error: String? = null,
    val message: String? = null
)