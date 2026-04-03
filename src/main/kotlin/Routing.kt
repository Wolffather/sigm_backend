package ru.hey_savvy

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
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
import ru.hey_savvy.tables.UsersTable
import java.util.Date
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import ru.hey_savvy.model.ProfileUpdate
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

        post("/register") {
            val user = call.receive<User>()
            val username = user.username
            val password = user.password

            val exists = userService.exists(username)
            if (exists) {
                call.respond(HttpStatusCode.Conflict, "${user.username} already exists")
            } else {
                val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
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
            val isPasswordValid = BCrypt.verifyer().verify(
                incoming.password.toCharArray(),
                found[UsersTable.password]
            ).verified

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

        webSocket("/chat/{roomId}") {
            val token = call.request.queryParameters["token"]
            if (token == null) {
                close()
                return@webSocket
            }

            try {
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
                    .verify(token)
            } catch (e: Exception) {
                close()
                return@webSocket
            }

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

        authenticate("auth-jwt") {
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

            get("/messages/{roomId}") {
                val roomId = call.parameters["roomId"]?.toLongOrNull()
                if (roomId == null) {
                    call.respond(HttpStatusCode.NotFound, "Room not found")
                } else {
                    val messagesInRoom = messageService.getByRoom(roomId)
                    call.respond(HttpStatusCode.OK, messagesInRoom)
                }
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
    }
}

suspend fun broadcastToRoom(roomId: Long, frame: Frame) {
    if (frame is Frame.Text) {
        val text = frame.readText()
        val message = Json.decodeFromString<Message>(text)
        val author = message.author

        messageService.save(text, author, roomId)
        roomConnections[roomId]?.forEach { it.send(Frame.Text(text)) }
    }
}

