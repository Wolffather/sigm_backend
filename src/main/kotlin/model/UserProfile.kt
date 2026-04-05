package ru.hey_savvy.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val username: String,
    val status: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String,
    val firstName: String,
    val lastName: String
)
@Serializable
enum class UserStatus {
    AVAILABLE,
    BUSY,
    DO_NOT_DISTURB,
    AWAY,
    INVISIBLE
}