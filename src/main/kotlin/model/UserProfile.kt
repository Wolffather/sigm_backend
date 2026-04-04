package ru.hey_savvy.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val username: String,
    val status: String,
    val avatarUrl: String,
    val firstName: String,
    val lastName: String
)