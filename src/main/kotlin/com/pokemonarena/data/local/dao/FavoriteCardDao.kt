package com.pokemonarena.data.local.dao

import com.pokemonarena.data.local.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class FavoriteCardRow(
    val cardId: String, val pokemonName: String,
    val imageSmall: String, val rarity: String?, val setName: String,
    val statHp: Int = 0, val statAtk: Int = 0, val statDef: Int = 0,
    val statSpAtk: Int = 0, val statSpDef: Int = 0, val statSpd: Int = 0,
    val primaryType: String = "normal",
    val inTeam: Boolean = false,
    val heldItemId: String? = null,
    val timesUsed: Int = 0
)

class FavoriteCardDao {
    private val _changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun observeAll(): Flow<List<FavoriteCardRow>> = _changes.map {
        transaction { FavoriteCards.selectAll().orderBy(FavoriteCards.pokemonName).map { it.toRow() } }
    }.flowOn(Dispatchers.IO)

    fun observeTeam(): Flow<List<FavoriteCardRow>> = _changes.map {
        transaction { FavoriteCards.select { FavoriteCards.inTeam eq true }.map { it.toRow() } }
    }.flowOn(Dispatchers.IO)

    fun observeIsOwned(cardId: String): Flow<Boolean> = _changes.map {
        transaction { FavoriteCards.select { FavoriteCards.cardId eq cardId }.count() > 0 }
    }.flowOn(Dispatchers.IO)

    suspend fun insert(row: FavoriteCardRow) = withContext(Dispatchers.IO) {
        transaction {
            FavoriteCards.insertIgnore {
                it[FavoriteCards.cardId]      = row.cardId
                it[FavoriteCards.pokemonName] = row.pokemonName
                it[FavoriteCards.imageSmall]  = row.imageSmall
                it[FavoriteCards.rarity]      = row.rarity
                it[FavoriteCards.setName]     = row.setName
                it[FavoriteCards.statHp]      = row.statHp
                it[FavoriteCards.statAtk]     = row.statAtk
                it[FavoriteCards.statDef]     = row.statDef
                it[FavoriteCards.statSpAtk]   = row.statSpAtk
                it[FavoriteCards.statSpDef]   = row.statSpDef
                it[FavoriteCards.statSpd]     = row.statSpd
                it[FavoriteCards.primaryType] = row.primaryType
            }
        }
        _changes.emit(Unit)
    }

    suspend fun delete(cardId: String) = withContext(Dispatchers.IO) {
        transaction { FavoriteCards.deleteWhere { FavoriteCards.cardId eq cardId } }
        _changes.emit(Unit)
    }

    suspend fun updateTeam(cardId: String, inTeam: Boolean) = withContext(Dispatchers.IO) {
        transaction {
            FavoriteCards.update({ FavoriteCards.cardId eq cardId }) { it[FavoriteCards.inTeam] = inTeam }
        }
        _changes.emit(Unit)
    }

    suspend fun updateHeldItem(cardId: String, itemId: String?) = withContext(Dispatchers.IO) {
        transaction {
            FavoriteCards.update({ FavoriteCards.cardId eq cardId }) { it[FavoriteCards.heldItem] = itemId }
        }
        _changes.emit(Unit)
    }

    suspend fun resetTimesUsed(cardId: String) = withContext(Dispatchers.IO) {
        transaction {
            FavoriteCards.update({ FavoriteCards.cardId eq cardId }) { it[timesUsed] = 0 }
        }
        _changes.emit(Unit)
    }

    suspend fun incrementTimesUsed(cardIds: List<String>) = withContext(Dispatchers.IO) {
        transaction {
            cardIds.forEach { id ->
                FavoriteCards.update({ FavoriteCards.cardId eq id }) {
                    with(SqlExpressionBuilder) { it[timesUsed] = timesUsed + 1 }
                }
            }
        }
        _changes.emit(Unit)
    }

    private fun ResultRow.toRow() = FavoriteCardRow(
        cardId      = this[FavoriteCards.cardId],
        pokemonName = this[FavoriteCards.pokemonName],
        imageSmall  = this[FavoriteCards.imageSmall],
        rarity      = this[FavoriteCards.rarity],
        setName     = this[FavoriteCards.setName],
        statHp      = this[FavoriteCards.statHp],
        statAtk     = this[FavoriteCards.statAtk],
        statDef     = this[FavoriteCards.statDef],
        statSpAtk   = this[FavoriteCards.statSpAtk],
        statSpDef   = this[FavoriteCards.statSpDef],
        statSpd     = this[FavoriteCards.statSpd],
        primaryType = this[FavoriteCards.primaryType],
        inTeam      = this[FavoriteCards.inTeam],
        heldItemId  = this[FavoriteCards.heldItem],
        timesUsed   = this[FavoriteCards.timesUsed]
    )
}
