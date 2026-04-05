package ru.hey_savvy.services

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.hey_savvy.model.ProfileUpdate
import ru.hey_savvy.model.UserProfile
import ru.hey_savvy.model.UserStatus
import ru.hey_savvy.tables.UsersTable
import ru.hey_savvy.utils.PasswordUtils

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
                status = UserStatus.valueOf(it[UsersTable.status].ifBlank { "AVAILABLE" }),
                avatarUrl = it[UsersTable.avatarUrl],
                firstName = it[UsersTable.firstName],
                lastName = it[UsersTable.lastName]
            )
        }
    }

    fun updateProfile(id: Long, update: ProfileUpdate) = transaction {
        UsersTable.update({ UsersTable.id eq id }) {
            update.status?.let { s -> it[status] = s.name }
            update.avatarUrl?.let { a -> it[avatarUrl] = a }
            update.firstName?.let { f -> it[firstName] = f }
            update.lastName?.let { l -> it[lastName] = l }
        }
    }

    fun changePassword(id: Long, currentPassword: String, newPassword: String): Boolean = transaction {
        val user = UsersTable.selectAll().where { UsersTable.id eq id }.firstOrNull()
            ?: return@transaction false

        val isValid = PasswordUtils.verify(currentPassword, user[UsersTable.password])

        if (!isValid) return@transaction false

        UsersTable.update({ UsersTable.id eq id }) {
            it[password] = PasswordUtils.hash(newPassword)
        }
        true
    }

    fun changeUsername(id: Long, newUsername: String, currentPassword: String): Boolean = transaction {
        val user = UsersTable.selectAll().where { UsersTable.id eq id }.firstOrNull()
            ?: return@transaction false

        val isValid = PasswordUtils.verify(currentPassword, user[UsersTable.password])

        if (!isValid) return@transaction false

        val exists = UsersTable.selectAll().where { UsersTable.username eq newUsername }.count() > 0
        if (exists) return@transaction false

        UsersTable.update({ UsersTable.id eq id }) {
            it[username] = newUsername
        }
        true
    }

}