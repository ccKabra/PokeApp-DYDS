package com.pokemonarena.data.local.dao

import com.pokemonarena.data.local.database.UserProfiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class UserProfileRow(val name: String, val gender: String)

class UserProfileDao {
    private val _changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun observe(): Flow<UserProfileRow?> = _changes.map {
        transaction {
            UserProfiles.select { UserProfiles.id eq 1 }.firstOrNull()?.let {
                UserProfileRow(it[UserProfiles.name], it[UserProfiles.gender])
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun upsert(row: UserProfileRow) = withContext(Dispatchers.IO) {
        transaction {
            val exists = UserProfiles.select { UserProfiles.id eq 1 }.count() > 0
            if (exists) {
                UserProfiles.update({ UserProfiles.id eq 1 }) {
                    it[name]   = row.name
                    it[gender] = row.gender
                }
            } else {
                UserProfiles.insert {
                    it[id]     = 1
                    it[name]   = row.name
                    it[gender] = row.gender
                }
            }
        }
        _changes.emit(Unit)
    }
}
