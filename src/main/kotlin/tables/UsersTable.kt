package ru.hey_savvy.tables

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255)
    val status = varchar("status", 255).default("")
    val avatarUrl = varchar("avatar_url", 500).default("")
    val firstName = varchar("first_name", 255).default("")
    val lastName = varchar("last_name", 255).default("")
    override val primaryKey = PrimaryKey(id)
}