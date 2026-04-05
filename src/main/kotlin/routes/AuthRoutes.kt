package ru.hey_savvy.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import ru.hey_savvy.model.User
import ru.hey_savvy.tables.UsersTable
import ru.hey_savvy.userService
import ru.hey_savvy.utils.PasswordUtils
import java.util.Date

fun Route.authRoutes(jwtSecret: String, jwtIssuer: String, jwtAudience: String) {
    post("/register") {
        val user = call.receive<User>()
        val username = user.username
        val password = user.password

        val exists = userService.exists(username)
        if (exists) {
            call.respond(HttpStatusCode.Conflict, "${user.username} already exists")
        } else {
            val hashedPassword = PasswordUtils.hash(password)
            userService.create(username, hashedPassword)
            call.respond(HttpStatusCode.Created, "User ${user.username} registered")
        }
    }

    post("/login") {
        val incoming = call.receive<User>()
        val username = incoming.username

        val found = userService.findByUsername(username)
        if (found == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
            return@post
        }
        val isPasswordValid = PasswordUtils.verify(incoming.password, found[UsersTable.password])

        if (!isPasswordValid) {
            call.respond(HttpStatusCode.Unauthorized, "Wrong password")
            return@post
        }

        val token = JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("username", incoming.username)
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000L))
            .sign(Algorithm.HMAC256(jwtSecret))

        call.respond(hashMapOf("token" to token))
    }
}