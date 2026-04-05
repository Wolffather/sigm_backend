package ru.hey_savvy.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import ru.hey_savvy.getUserId
import ru.hey_savvy.roomService
import ru.hey_savvy.tables.UsersTable
import ru.hey_savvy.userService

fun Route.chatRoutes() {
    post("/chats/private/{username}") {
        val targetUsername = call.parameters["username"]
            ?: run { call.respond(HttpStatusCode.BadRequest, "Username required"); return@post }

        val userId = call.getUserId(userService)
            ?: run { call.respond(HttpStatusCode.Unauthorized); return@post }

        val targetUserId = userService.findByUsername(targetUsername)?.get(UsersTable.id)
            ?: run { call.respond(HttpStatusCode.NotFound, "User not found"); return@post }

        val existing = roomService.findPrivateChat(userId, targetUserId)
        val room = if (existing != null) {
            roomService.getRoomById(existing)!!
        } else {
            roomService.createPrivateChat(userId, targetUserId)
        }

        call.respond(HttpStatusCode.OK, room)
    }
}