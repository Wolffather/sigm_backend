package ru.hey_savvy

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.hey_savvy.model.Message
import ru.hey_savvy.model.User

val users = mutableListOf<User>()
val messages = mutableListOf<Message>()

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/register") {

        }
        post("/login") {

        }
        get("/messages") {

        }
    }
}
