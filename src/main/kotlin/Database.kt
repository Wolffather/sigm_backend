package ru.hey_savvy

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ru.hey_savvy.tables.MessagesTable
import ru.hey_savvy.tables.RoomMembersTable
import ru.hey_savvy.tables.RoomsTable
import ru.hey_savvy.tables.UsersTable

fun configureDatabase() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5434/sigm_db",
        driver = "org.postgresql.Driver",
        user = "sigm_adm_usr",
        password = "sigm_adm_psswrd"
    )

    transaction {
        SchemaUtils.create(UsersTable, RoomsTable, MessagesTable, RoomMembersTable)
    }
}