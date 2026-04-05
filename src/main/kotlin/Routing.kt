package ru.hey_savvy

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*
import io.ktor.websocket.DefaultWebSocketSession
import ru.hey_savvy.routes.authRoutes
import ru.hey_savvy.routes.chatRoutes
import ru.hey_savvy.routes.messageRoutes
import ru.hey_savvy.routes.roomRoutes
import ru.hey_savvy.routes.userRoutes
import ru.hey_savvy.routes.webSocketRoutes
import ru.hey_savvy.services.MessageService
import ru.hey_savvy.services.RoomService
import ru.hey_savvy.services.UserService

val roomConnections = mutableMapOf<Long, MutableSet<DefaultWebSocketSession>>()
val userService: UserService = UserService()
val roomService: RoomService = RoomService()
val messageService: MessageService = MessageService()

fun Application.configureRouting(
    jwtSecret: String,
    jwtIssuer: String,
    jwtAudience: String
) {
    routing {
        authRoutes(jwtSecret, jwtIssuer, jwtAudience)
        webSocketRoutes(jwtIssuer, jwtAudience, jwtSecret)

        authenticate("auth-jwt") {
            userRoutes()
            roomRoutes()
            messageRoutes()
            chatRoutes()
        }
    }
}



