package ru.hey_savvy.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    private val username: String,
    private val password: String,
)
