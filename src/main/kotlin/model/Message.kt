package ru.hey_savvy.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    private val text: String,
    private val author: User,
)
