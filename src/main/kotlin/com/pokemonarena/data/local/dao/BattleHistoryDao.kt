package com.pokemonarena.data.local.dao

import com.pokemonarena.data.local.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class BattleHistoryRow(
    val gymName: String, val playerCardName: String, val botCardName: String,
    val winner: String, val playerScore: Float, val botScore: Float,
    val weatherCondition: String, val date: String, val coinsDelta: Int = 0
)

class BattleHistoryDao {
    private val _changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun observeAll(): Flow<List<BattleHistoryRow>> = _changes.map {
        transaction {
            BattleHistory.selectAll().orderBy(BattleHistory.date to SortOrder.DESC).map { it.toRow() }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun insert(row: BattleHistoryRow) = withContext(Dispatchers.IO) {
        transaction {
            BattleHistory.insert {
                it[gymName]          = row.gymName
                it[playerCardName]   = row.playerCardName
                it[botCardName]      = row.botCardName
                it[winner]           = row.winner
                it[playerScore]      = row.playerScore
                it[botScore]         = row.botScore
                it[weatherCondition] = row.weatherCondition
                it[date]             = row.date
                it[coinsDelta]       = row.coinsDelta
            }
        }
        _changes.emit(Unit)
    }

    private fun ResultRow.toRow() = BattleHistoryRow(
        gymName          = this[BattleHistory.gymName],
        playerCardName   = this[BattleHistory.playerCardName],
        botCardName      = this[BattleHistory.botCardName],
        winner           = this[BattleHistory.winner],
        playerScore      = this[BattleHistory.playerScore],
        botScore         = this[BattleHistory.botScore],
        weatherCondition = this[BattleHistory.weatherCondition],
        date             = this[BattleHistory.date],
        coinsDelta       = this[BattleHistory.coinsDelta]
    )
}
