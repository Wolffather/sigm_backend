package ru.hey_savvy.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import ru.hey_savvy.messageService
import kotlin.text.toLongOrNull

fun Route.messageRoutes() {
    get("/messages/{roomId}") {
        val roomId = call.parameters["roomId"]?.toLongOrNull()
        if (roomId == null) {
            call.respond(HttpStatusCode.NotFound, "Room not found")
        } else {
            val messagesInRoom = messageService.getByRoom(roomId)
            call.respond(HttpStatusCode.OK, messagesInRoom)
        }
    }

}