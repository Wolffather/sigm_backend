package ru.hey_savvy.services

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.hey_savvy.model.Room
import ru.hey_savvy.model.RoomType
import ru.hey_savvy.tables.MemberRole
import ru.hey_savvy.tables.RoomMembersTable
import ru.hey_savvy.tables.RoomsTable
import ru.hey_savvy.tables.UsersTable

class RoomService {
    fun getAll() = transaction {
        RoomsTable.selectAll().map {
            Room(it[RoomsTable.id], it[RoomsTable.name], RoomType.valueOf(it[RoomsTable.type]))
        }
    }

    fun getRoomById(id: Long): Room? = transaction {
        RoomsTable.selectAll().where { RoomsTable.id eq id }.firstOrNull()?.let {
            Room(it[RoomsTable.id], it[RoomsTable.name], RoomType.valueOf(it[RoomsTable.type]))
        }
    }

    fun getRoomsForUser(userId: Long) = transaction {
        RoomMembersTable.innerJoin(RoomsTable)
            .selectAll()
            .where { RoomMembersTable.userId eq userId }
            .map {
                Room(it[RoomsTable.id], it[RoomsTable.name], RoomType.valueOf(it[RoomsTable.type]))
            }
    }

    fun findPrivateChat(userId1: Long, userId2: Long): Long? {
        val members1 = RoomMembersTable.alias("members1")
        val members2 = RoomMembersTable.alias("members2")

        return transaction {
            RoomsTable
                .join(members1, JoinType.INNER, RoomsTable.id, members1[RoomMembersTable.roomId])
                .join(members2, JoinType.INNER, RoomsTable.id, members2[RoomMembersTable.roomId])
                .selectAll()
                .where {
                    (RoomsTable.type eq RoomType.PRIVATE.name) and
                            (members1[RoomMembersTable.userId] eq userId1) and
                            (members2[RoomMembersTable.userId] eq userId2)
                }
                .firstOrNull()
                ?.get(RoomsTable.id)
        }
    }

    fun createPrivateChat(userId1: Long, userId2: Long): Room = transaction {
        val result = RoomsTable.insert {
            it[name] = "private"
            it[type] = RoomType.PRIVATE.name
        }
        val roomId = result[RoomsTable.id]

        RoomMembersTable.insert {
            it[RoomMembersTable.userId] = userId1
            it[RoomMembersTable.roomId] = roomId
            it[role] = MemberRole.OWNER.name
        }
        RoomMembersTable.insert {
            it[RoomMembersTable.userId] = userId2
            it[RoomMembersTable.roomId] = roomId
            it[role] = MemberRole.OWNER.name
        }

        Room(roomId, "private", RoomType.PRIVATE)
    }

    fun create(room: Room, ownerId: Long): Room = transaction {
        val result = RoomsTable.insert {
            it[name] = room.name
            it[type] = room.type.name
        }
        val roomId = result[RoomsTable.id]
        RoomMembersTable.insert {
            it[RoomMembersTable.userId] = ownerId
            it[RoomMembersTable.roomId] = roomId
            it[role] = MemberRole.OWNER.name
        }
        Room(roomId, room.name, room.type)
    }

    fun join(userId: Long, roomId: Long) = transaction {
        RoomMembersTable.insert {
            it[RoomMembersTable.userId] = userId
            it[RoomMembersTable.roomId] = roomId
            it[role] = MemberRole.MEMBER.name
        }
    }

    fun getMembers(roomId: Long) = transaction {
        RoomMembersTable.innerJoin(UsersTable)
            .selectAll()
            .where { RoomMembersTable.roomId eq roomId }
            .map { it[UsersTable.username] }
    }
}