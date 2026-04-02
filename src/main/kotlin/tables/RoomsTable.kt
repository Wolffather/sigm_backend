package ru.hey_savvy.tables

import org.jetbrains.exposed.sql.Table

object RoomsTable : Table("rooms") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val type = varchar("type", 50)
    override val primaryKey = PrimaryKey(id)
}