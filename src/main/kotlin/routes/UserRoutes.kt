package ru.hey_savvy.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import ru.hey_savvy.getUserId
import ru.hey_savvy.model.ChangePasswordRequest
import ru.hey_savvy.model.ChangeUsernameRequest
import ru.hey_savvy.model.ProfileUpdate
import ru.hey_savvy.userService

fun Route.userRoutes() {
    get("/users/me") {
        val userId = call.getUserId(userService)
            ?: run { call.respond(HttpStatusCode.Unauthorized); return@get }

        val user = userService.findById(userId)
            ?: run { call.respond(HttpStatusCode.NotFound); return@get }

        call.respond(HttpStatusCode.OK, user)
    }

    put("/users/me") {
        val userId = call.getUserId(userService)
            ?: run { call.respond(HttpStatusCode.Unauthorized); return@put }

        val update = call.receive<ProfileUpdate>()
        userService.updateProfile(userId, update)
        call.respond(HttpStatusCode.OK, "Profile updated")
    }

    put("/users/me/password") {
        val userId = call.getUserId(userService)
            ?: run { call.respond(HttpStatusCode.Unauthorized); return@put }

        val request = call.receive<ChangePasswordRequest>()
        val success = userService.changePassword(userId, request.currentPassword, request.newPassword)

        if (success) {
            call.respond(HttpStatusCode.OK, "Password changed")
        } else {
            call.respond(HttpStatusCode.BadRequest, "Invalid current password")
        }
    }

    put("/users/me/username") {
        val userId = call.getUserId(userService)
            ?: run { call.respond(HttpStatusCode.Unauthorized); return@put }

        val request = call.receive<ChangeUsernameRequest>()
        val success = userService.changeUsername(userId, request.newUsername, request.currentPassword)

        if (success) {
            call.respond(HttpStatusCode.OK, "Username changed")
        } else {
            call.respond(HttpStatusCode.BadRequest, "Invalid password or username taken")
        }
    }
}