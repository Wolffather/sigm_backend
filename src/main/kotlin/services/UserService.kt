package ru.hey_savvy.services

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.hey_savvy.model.ProfileUpdate
import ru.hey_savvy.model.UserProfile
import ru.hey_savvy.tables.UsersTable

class UserService {
    fun findByUsername(username: String) = transaction {
        UsersTable.selectAll().where { UsersTable.username eq username }.firstOrNull()
    }

    fun create(username: String, hashedPassword: String) = transaction {
        UsersTable.insert {
            it[UsersTable.username] = username
            it[UsersTable.password] = hashedPassword
        }
    }

    fun exists(username: String) = transaction {
        UsersTable.selectAll().where { UsersTable.username eq username }.count() > 0
    }

    fun findById(id: Long): UserProfile? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq id }.firstOrNull()?.let {
            UserProfile(
                username = it[UsersTable.username],
                status = it[UsersTable.status],
                avatarUrl = it[UsersTable.avatarUrl],
                firstName = it[UsersTable.firstName],
                lastName = it[UsersTable.lastName]
            )
        }
    }

    fun updateProfile(id: Long, update: ProfileUpdate) = transaction {
        UsersTable.update({ UsersTable.id eq id }) {
            update.status?.let { s -> it[status] = s }
            update.avatarUrl?.let { a -> it[avatarUrl] = a }
            update.firstName?.let { f -> it[firstName] = f }
            update.lastName?.let { l -> it[lastName] = l }
        }
    }
}