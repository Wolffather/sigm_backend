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
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.hey_savvy.model.Message
import ru.hey_savvy.model.Room
import ru.hey_savvy.model.RoomType
import ru.hey_savvy.model.User
import ru.hey_savvy.tables.MessagesTable
import ru.hey_savvy.tables.RoomsTable
import ru.hey_savvy.tables.UsersTable
import java.util.Date

val roomConnections = mutableMapOf<Long, MutableSet<DefaultWebSocketSession>>()

fun Application.configureRouting(
    jwtSecret: String,
    jwtIssuer: String,
    jwtAudience: String
) {
    routing {
        post("/register") {
            val user = call.receive<User>()
            val exists = transaction {
                UsersTable.selectAll().where { UsersTable.username eq user.username }.count() > 0
            }
            if (exists) {
                call.respond(HttpStatusCode.Conflict, "${user.username} already exists")
            } else {
                transaction {
                    UsersTable.insert {
                        it[username] = user.username
                        it[password] = BCrypt.withDefaults().hashToString(12, user.password.toCharArray())
                    }
                }
                call.respond(HttpStatusCode.Created, "User ${user.username} registered")
            }
        }
        post("/login") {
            val incoming = call.receive<User>()
            val found = transaction {
                UsersTable.selectAll().where { UsersTable.username eq incoming.username }.firstOrNull()
            }
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
            get("/messages/{roomId}") {
                val roomId = call.parameters["roomId"]?.toLongOrNull()
                if (roomId == null) {
                    call.respond(HttpStatusCode.NotFound, "Room not found")
                } else {
                    val messagesInRoom = transaction {
                        MessagesTable.selectAll()
                            .where { MessagesTable.roomId eq roomId }
                            .map {
                                Message(
                                    text = it[MessagesTable.text],
                                    author = it[MessagesTable.author],
                                    roomId = it[MessagesTable.roomId]
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, messagesInRoom)
                }
            }
            get("/rooms") {
                val roomList = transaction {
                    RoomsTable.selectAll().map {
                        Room(
                            id = it[RoomsTable.id],
                            name = it[RoomsTable.name],
                            type = RoomType.valueOf(it[RoomsTable.type])
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, roomList)
            }

            post("/rooms") {
                val room = call.receive<Room>()
                val newRoom = transaction {
                    val result = RoomsTable.insert {
                        it[name] = room.name
                        it[type] = room.type.name
                    }
                    Room(
                        id = result[RoomsTable.id],
                        name = room.name,
                        type = room.type
                    )
                }
                call.respond(HttpStatusCode.Created, newRoom)
            }
        }
    }
}

suspend fun broadcastToRoom(roomId: Long, frame: Frame) {
    if (frame is Frame.Text) {
        val text = frame.readText()
        val message = Json.decodeFromString<Message>(text)

        transaction {
            MessagesTable.insert {
                it[MessagesTable.text] = message.text
                it[MessagesTable.author] = message.author
                it[MessagesTable.roomId] = roomId
            }
        }
        roomConnections[roomId]?.forEach { it.send(Frame.Text(text)) }
    }
}
