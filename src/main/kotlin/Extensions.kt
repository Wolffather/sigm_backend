package ru.hey_savvy

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import ru.hey_savvy.services.UserService
import ru.hey_savvy.tables.UsersTable

fun ApplicationCall.getUserId(userService: UserService): Long? {
    val username = principal<JWTPrincipal>()?.payload?.getClaim("username")?.asString()
        ?: return null
    return userService.findByUsername(username)?.get(UsersTable.id)
}

fun ApplicationCall.getRoomAndUser(userService: UserService): Pair<Long, Long>? {
    val roomId = parameters["id"]?.toLongOrNull() ?: return null
    val userId = getUserId(userService) ?: return null
    return Pair(roomId, userId)
}