package ru.hey_savvy.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val text: String,
    val author: String = "Unknown",
    val roomId: Long = 0L
)
