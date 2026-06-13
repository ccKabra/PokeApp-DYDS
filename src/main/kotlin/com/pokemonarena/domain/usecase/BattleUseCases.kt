package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.*
import com.pokemonarena.domain.repository.BattleRepository
import com.pokemonarena.domain.repository.CardRepository
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class SimulateBattleUseCase(private val random: Random = Random.Default) {

    fun execute(
        playerCards: List<Card>, botCards: List<Card>,
        weather: WeatherCondition, gym: Gym, date: String = ""
    ): BattleResult {
        val rounds = playerCards.zip(botCards).map { (player, bot) -> playRound(player, bot, weather) }

        val playerTotal = rounds.sumOf { it.playerScore.toDouble() }.toFloat()
        val botTotal    = rounds.sumOf { it.botScore.toDouble() }.toFloat()
        val playerWins  = rounds.count { it.winner == Winner.PLAYER }
        val botWins     = rounds.count { it.winner == Winner.BOT }

        val winner = when {
            playerWins > botWins    -> Winner.PLAYER
            botWins > playerWins    -> Winner.BOT
            playerTotal > botTotal  -> Winner.PLAYER
            botTotal > playerTotal  -> Winner.BOT
            else                    -> Winner.DRAW
        }

        return BattleResult(
            winner        = winner,
            playerCard    = playerCards.first(), botCard = botCards.first(),
            playerCards   = playerCards,         botCards = botCards,
            playerScore   = playerTotal,         botScore = botTotal,
            weatherCondition = weather,
            gymName       = gym.name,
            date          = date,
            rounds        = rounds,
            coinsDelta    = BattleRewards.coinsFor(winner, playerTotal, botTotal, gym.difficulty),
            gymDifficulty = gym.difficulty
        )
    }

    private fun playRound(player: Card, bot: Card, weather: WeatherCondition): RoundResult {
        val missChance    = (PLAYER_MISS_CHANCE - (player.heldItem?.missReduction ?: 0f)).coerceAtLeast(0f)
        val playerMissed  = random.nextFloat() < missChance
        val playerMatchup = TypeMatchup.multiplier(player.primaryType, bot.primaryType)
        val botMatchup    = TypeMatchup.multiplier(bot.primaryType, player.primaryType)
        val playerCrit    = !playerMissed && random.nextFloat() < PLAYER_CRIT_CHANCE
        val botCrit       = random.nextFloat() < GYM_CRIT_CHANCE

        val playerScore = if (playerMissed) 0f
                          else scoreOf(player, weather) * playerMatchup * critMultiplier(playerCrit)
        val botScore    = scoreOf(bot, weather) * botMatchup * critMultiplier(botCrit)

        val winner = when {
            playerScore > botScore -> Winner.PLAYER
            botScore > playerScore -> Winner.BOT
            else                   -> Winner.DRAW
        }
        return RoundResult(player, bot, playerScore, botScore, winner,
                           playerCrit, botCrit, playerMissed, playerMatchup, botMatchup)
    }

    fun scoreOf(card: Card, w: WeatherCondition): Float =
        BattleScore.weightedOf(card.effectiveStats) *
            (TypeEffectiveness.multiplierFor(card.primaryType, w) ?: 1.0f)

    private fun critMultiplier(crit: Boolean) = if (crit) CRIT_MULTIPLIER else 1.0f

    companion object {
        const val PLAYER_CRIT_CHANCE = 0.10f
        const val GYM_CRIT_CHANCE    = 0.20f
        const val PLAYER_MISS_CHANCE = 0.30f
        const val CRIT_MULTIPLIER    = BattleScore.CRIT_MULTIPLIER
    }
}

class UpdateStatisticsAfterBattleUseCase(private val repo: BattleRepository) {
    suspend fun execute(result: BattleResult) {
        val current = repo.getUserStatistics().first()
        val history = repo.getBattleHistory().first()
        val streak  = history.map { it.winner }.takeWhile { it == Winner.PLAYER }.size
        val fav     = history.groupingBy { it.playerCard.pokemonName }.eachCount().maxByOrNull { it.value }?.key
        repo.saveUserStatistics(current.copy(
            totalBattles    = current.totalBattles + 1,
            totalWins       = current.totalWins   + if (result.playerWon) 1 else 0,
            totalLosses     = current.totalLosses + if (!result.playerWon && !result.isDraw) 1 else 0,
            currentStreak   = streak,
            bestStreak      = maxOf(current.bestStreak, streak),
            favoritePokemon = fav ?: current.favoritePokemon,
            coins           = (current.coins + result.coinsDelta).coerceAtLeast(0)
        ))
    }
}

class SaveBattleResultUseCase(
    private val repo:        BattleRepository,
    private val cardRepo:    CardRepository,
    private val updateStats: UpdateStatisticsAfterBattleUseCase
) {
    suspend fun execute(result: BattleResult) {
        repo.saveBattleResult(result)
        cardRepo.registerBattleUsage(result.playerCards.map { it.id })
        updateStats.execute(result)
    }
}
