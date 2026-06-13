package com.pokemonarena.data.repository

import com.pokemonarena.data.local.dao.BattleHistoryDao
import com.pokemonarena.data.local.dao.BattleHistoryRow
import com.pokemonarena.data.local.dao.UserStatisticsDao
import com.pokemonarena.data.local.dao.UserStatisticsRow
import com.pokemonarena.domain.entity.*
import com.pokemonarena.domain.repository.BattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class BattleRepositoryImpl(
    private val historyDao: BattleHistoryDao,
    private val statsDao:   UserStatisticsDao
) : BattleRepository {

    override suspend fun saveBattleResult(result: BattleResult) {
        val date = result.date.ifBlank { LocalDateTime.now().toString() }
        historyDao.insert(BattleHistoryRow(
            gymName          = result.gymName,
            playerCardName   = result.playerCard.name,
            botCardName      = result.botCard.name,
            winner           = result.winner.name,
            playerScore      = result.playerScore,
            botScore         = result.botScore,
            weatherCondition = result.weatherCondition.name,
            date             = date,
            coinsDelta       = result.coinsDelta
        ))
    }

    override suspend fun saveUserStatistics(stats: UserStatistics) {
        statsDao.upsert(UserStatisticsRow(
            totalBattles    = stats.totalBattles,
            totalWins       = stats.totalWins,
            totalLosses     = stats.totalLosses,
            currentStreak   = stats.currentStreak,
            bestStreak      = stats.bestStreak,
            favoritePokemon = stats.favoritePokemon,
            coins           = stats.coins
        ))
    }

    override fun getBattleHistory(): Flow<List<BattleResult>> =
        historyDao.observeAll().map { rows ->
            rows.map { r ->
                BattleResult(
                    winner           = runCatching { Winner.valueOf(r.winner.uppercase()) }
                                           .getOrDefault(Winner.DRAW),
                    playerCard       = stubCard(r.playerCardName),
                    botCard          = stubCard(r.botCardName),
                    playerCards      = emptyList(),
                    botCards         = emptyList(),
                    playerScore      = r.playerScore,
                    botScore         = r.botScore,
                    weatherCondition = runCatching { WeatherCondition.valueOf(r.weatherCondition) }
                                           .getOrDefault(WeatherCondition.CLEAR),
                    gymName          = r.gymName,
                    date             = r.date,
                    coinsDelta       = r.coinsDelta
                )
            }
        }

    override fun getUserStatistics(): Flow<UserStatistics> =
        statsDao.observe().map { row ->
            row?.let {
                UserStatistics(it.totalBattles, it.totalWins, it.totalLosses,
                               it.currentStreak, it.bestStreak, it.favoritePokemon, it.coins)
            } ?: UserStatistics()
        }

    private fun stubCard(name: String): Card {
        val stub = PokemonDetail(0, name, "", emptyList(), 0, 0, Stats(0,0,0,0,0,0), emptyList())
        return Card("", name, "", "", null, "", stub)
    }
}
