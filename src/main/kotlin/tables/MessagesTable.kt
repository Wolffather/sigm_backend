package ru.hey_savvy.tables

import org.jetbrains.exposed.sql.Table

object MessagesTable : Table("messages") {
    val id = long("id").autoIncrement()
    val text = text("text")
    val author = varchar("author", 255)
    val roomId = long("room_id").references(RoomsTable.id)
    override val primaryKey = PrimaryKey(id)
}