package com.pokemonarena.data.local.dao

import com.pokemonarena.data.local.database.*
import com.pokemonarena.domain.entity.Economy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class UserStatisticsRow(
    val totalBattles: Int = 0, val totalWins: Int = 0, val totalLosses: Int = 0,
    val currentStreak: Int = 0, val bestStreak: Int = 0,
    val favoritePokemon: String? = null,
    val coins: Int = Economy.STARTING_COINS
)

class UserStatisticsDao {
    private val _changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun observe(): Flow<UserStatisticsRow?> = _changes.map {
        transaction {
            UserStatisticsTable.select { UserStatisticsTable.id eq 1 }.firstOrNull()?.toRow()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getOnce(): UserStatisticsRow? = withContext(Dispatchers.IO) {
        transaction { UserStatisticsTable.select { UserStatisticsTable.id eq 1 }.firstOrNull()?.toRow() }
    }

    suspend fun upsert(row: UserStatisticsRow) = withContext(Dispatchers.IO) {
        transaction {
            val exists = UserStatisticsTable.select { UserStatisticsTable.id eq 1 }.count() > 0
            if (exists) {
                UserStatisticsTable.update({ UserStatisticsTable.id eq 1 }) {
                    it[totalBattles]    = row.totalBattles
                    it[totalWins]       = row.totalWins
                    it[totalLosses]     = row.totalLosses
                    it[currentStreak]   = row.currentStreak
                    it[bestStreak]      = row.bestStreak
                    it[favoritePokemon] = row.favoritePokemon
                    it[coins]           = row.coins
                }
            } else {
                UserStatisticsTable.insert {
                    it[id]              = 1
                    it[totalBattles]    = row.totalBattles
                    it[totalWins]       = row.totalWins
                    it[totalLosses]     = row.totalLosses
                    it[currentStreak]   = row.currentStreak
                    it[bestStreak]      = row.bestStreak
                    it[favoritePokemon] = row.favoritePokemon
                    it[coins]           = row.coins
                }
            }
        }
        _changes.emit(Unit)
    }

    private fun ResultRow.toRow() = UserStatisticsRow(
        totalBattles    = this[UserStatisticsTable.totalBattles],
        totalWins       = this[UserStatisticsTable.totalWins],
        totalLosses     = this[UserStatisticsTable.totalLosses],
        currentStreak   = this[UserStatisticsTable.currentStreak],
        bestStreak      = this[UserStatisticsTable.bestStreak],
        favoritePokemon = this[UserStatisticsTable.favoritePokemon],
        coins           = this[UserStatisticsTable.coins]
    )
}
