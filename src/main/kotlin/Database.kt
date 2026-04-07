package ru.hey_savvy

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ru.hey_savvy.tables.MessagesTable
import ru.hey_savvy.tables.RoomMembersTable
import ru.hey_savvy.tables.RoomsTable
import ru.hey_savvy.tables.UsersTable

fun configureDatabase() {
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5434/sigm_db"
    val dbUser = System.getenv("DB_USER") ?: "sigm_adm_usr"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "sigm_adm_psswrd"

    Database.connect(
        url = dbUrl,
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )

    transaction {
        SchemaUtils.create(UsersTable, RoomsTable, MessagesTable, RoomMembersTable)
    }
}