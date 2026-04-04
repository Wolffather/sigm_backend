package ru.hey_savvy.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdate(
    val status: String? = null,
    val avatarUrl: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)