package ru.hey_savvy.tables

import org.jetbrains.exposed.sql.Table

enum class MemberRole {
    MEMBER,
    ADMIN,
    SUBSCRIBER,
    OWNER
}

object RoomMembersTable : Table("room_members") {
    val userId = long("user_id").references(UsersTable.id)
    val roomId = long("room_id").references(RoomsTable.id)
    val role = varchar("role", 50).default(MemberRole.MEMBER.name)

    override val primaryKey = PrimaryKey(userId, roomId)
}