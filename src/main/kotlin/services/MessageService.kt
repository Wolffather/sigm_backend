package ru.hey_savvy.services

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.hey_savvy.model.Message
import ru.hey_savvy.tables.MessagesTable

class MessageService {
    fun getByRoom(roomId: Long) = transaction {
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

    fun save(text: String, author: String, roomId: Long) = transaction {
        MessagesTable.insert {
            it[MessagesTable.text] = text
            it[MessagesTable.author] = author
            it[MessagesTable.roomId] = roomId
        }
    }
}