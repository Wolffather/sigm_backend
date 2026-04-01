package ru.hey_savvy

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import ru.hey_savvy.model.Message
import ru.hey_savvy.model.Room
import ru.hey_savvy.model.User

val users = mutableListOf<User>()
val messages = mutableListOf<Message>()
val roomConnections = mutableMapOf<Long, MutableSet<DefaultWebSocketSession>>()
val rooms = mutableListOf<Room>()

fun Application.configureRouting() {
    routing {
        post("/register") {
            val user = call.receive<User>()
            if (users.any { it.username == user.username }) call.respond(
                HttpStatusCode.Conflict,
                "${user.username} already exists"
            )
            else {
                users.add(user)
                call.respond(HttpStatusCode.Created, "User ${user.username} registered")
            }
        }
        post("/login") {
            val incoming = call.receive<User>()
            val found = users.find { it.username == incoming.username }

            if (found == null) {
                call.respond(HttpStatusCode.NotFound, "User not found")
                return@post
            }

            if (found.password != incoming.password) {
                call.respond(HttpStatusCode.Unauthorized, "Wrong password")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Welcome!")
        }
        get("/messages/{roomId}") {
            val roomId = call.parameters["roomId"]?.toLongOrNull()
            if (roomId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid roomId")
            } else {
                val messagesInRoom = messages.filter { it.roomId == roomId }
                call.respond(HttpStatusCode.OK, messagesInRoom)
            }
        }
        webSocket("/chat/{roomId}") {
            val roomId = call.parameters["roomId"]?.toLongOrNull()
            if (roomId == null) {
                close()
                return@webSocket
            }

            val connectionSet = roomConnections.getOrPut(roomId) { mutableSetOf() }
            connectionSet.add(this)

            try {
                for (frame in incoming) broadcastToRoom(roomId, frame)
            } finally {
                connectionSet.remove(this)
            }
        }
        get("/rooms") {
            call.respond(HttpStatusCode.OK, rooms)
        }
        post("/rooms") {
            val body = call.receiveText()
            println("Received: $body")
            val room = Json.decodeFromString<Room>(body)
            val newRoom = room.copy(id = (rooms.size + 1).toLong())
            rooms.add(newRoom)
            call.respond(HttpStatusCode.Created, newRoom)
        }
    }
}

suspend fun broadcastToRoom(roomId: Long, frame: Frame) {
    if (frame is Frame.Text) {
        val text = frame.readText()
        val message = Json.decodeFromString<Message>(text)
        val messageWithRoom = message.copy(roomId = roomId)
        messages.add(messageWithRoom)
        roomConnections[roomId]?.forEach { it.send(Frame.Text(text)) }
    }
}
