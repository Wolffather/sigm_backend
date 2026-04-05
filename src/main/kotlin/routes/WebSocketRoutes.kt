package ru.hey_savvy.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.close
import ru.hey_savvy.roomConnections
import ru.hey_savvy.utils.BroadcastUtils.broadcastToRoom
import kotlin.text.toLongOrNull

fun Route.webSocketRoutes(jwtSecret: String, jwtIssuer: String, jwtAudience: String) {
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
}