package ru.hey_savvy.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: Long = 0L,
    val name: String,
    val type: RoomType
)

@Serializable
enum class RoomType{
    GROUP,
    CHANNEL,
    CHAT
}
