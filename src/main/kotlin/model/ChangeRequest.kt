package ru.hey_savvy.model

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class ChangeUsernameRequest(
    val newUsername: String,
    val currentPassword: String
)