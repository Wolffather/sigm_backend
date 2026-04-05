package ru.hey_savvy.utils

import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import ru.hey_savvy.messageService
import ru.hey_savvy.model.Message
import ru.hey_savvy.roomConnections

object BroadcastUtils {

    suspend fun broadcastToRoom(roomId: Long, frame: Frame) {
        if (frame is Frame.Text) {
            val text = frame.readText()
            val message = Json.decodeFromString<Message>(text)

            messageService.save(message.text, message.author, roomId)
            roomConnections[roomId]?.forEach { it.send(Frame.Text(text)) }
        }
    }
}