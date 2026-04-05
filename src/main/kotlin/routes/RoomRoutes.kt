package ru.hey_savvy.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import ru.hey_savvy.getUserId
import ru.hey_savvy.model.Room
import ru.hey_savvy.roomService
import ru.hey_savvy.tables.UsersTable
import ru.hey_savvy.userService
import kotlin.text.toLongOrNull

fun Route.roomRoutes() {
    post("/rooms/{id}/join") {
        val roomId = call.parameters["id"]?.toLongOrNull()
        if (roomId == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid room id")
            return@post
        }

        val username = call.principal<JWTPrincipal>()?.payload?.getClaim("username")?.asString()
        if (username == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }

        val userId = userService.findByUsername(username)?.get(UsersTable.id)

        if (userId == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
            return@post
        }

        roomService.join(userId, roomId)

        call.respond(HttpStatusCode.OK, "Joined")
    }

    get("/rooms/{id}/members") {
        val roomId = call.parameters["id"]?.toLongOrNull()
        if (roomId == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid room id")
            return@get
        }

        val members = roomService.getMembers(roomId)
        call.respond(HttpStatusCode.OK, members)
    }

    get("/rooms") {
        val userId = call.getUserId(userService)
            ?: run { call.respond(HttpStatusCode.Unauthorized); return@get }

        val roomList = roomService.getRoomsForUser(userId)
        call.respond(HttpStatusCode.OK, roomList)
    }

    get("/rooms/public") {
        val roomList = roomService.getAll()
        call.respond(HttpStatusCode.OK, roomList)
    }

    post("/rooms") {
        val room = call.receive<Room>()

        val userId = call.getUserId(userService)
            ?: run { call.respond(HttpStatusCode.Unauthorized); return@post }

        val newRoom = roomService.create(room, userId)
        call.respond(HttpStatusCode.Created, newRoom)
    }
}