package com.pokemonarena.domain.entity

import kotlin.math.roundToInt
import kotlin.random.Random

enum class Winner { PLAYER, BOT, DRAW }

object Economy {
    const val STARTING_COINS = 1250
}

object CollectionRules {
    const val MAX_OWNED_CARDS = 8
}

object TeamRules {
    const val SIZE = 3
}

data class Pokemon(
    val id: Int, val name: String,
    val imageUrl: String, val types: List<String>
) {
    val displayName: String get() = name.replaceFirstChar { it.uppercase() }
    val primaryType: String get() = types.firstOrNull() ?: "normal"
}

data class Stats(
    val hp: Int, val attack: Int, val defense: Int,
    val specialAttack: Int, val specialDefense: Int, val speed: Int
) {
    val total: Int get() = hp + attack + defense + specialAttack + specialDefense + speed

    fun scaledBy(factor: Float): Stats = Stats(
        hp             = (hp             * factor).roundToInt(),
        attack         = (attack         * factor).roundToInt(),
        defense        = (defense        * factor).roundToInt(),
        specialAttack  = (specialAttack  * factor).roundToInt(),
        specialDefense = (specialDefense * factor).roundToInt(),
        speed          = (speed          * factor).roundToInt()
    )
}

data class PokemonDetail(
    val id: Int, val name: String, val imageUrl: String,
    val types: List<String>, val height: Int, val weight: Int,
    val stats: Stats, val evolutionChain: List<String>
) {
    val displayName:    String get() = name.replaceFirstChar { it.uppercase() }
    val primaryType:    String get() = types.firstOrNull() ?: "normal"
    val heightInMeters: Float  get() = height / 10f
    val weightInKg:     Float  get() = weight / 10f
}

data class Card(
    val id: String, val name: String,
    val imageUrlSmall: String, val imageUrlLarge: String,
    val rarity: String?, val setName: String,
    val pokemonDetail: PokemonDetail,
    val heldItem: Item? = null,
    val timesUsed: Int = 0
) {
    val stats:       Stats        get() = pokemonDetail.stats
    val types:       List<String> get() = pokemonDetail.types
    val pokemonName: String       get() = pokemonDetail.name
    val primaryType: String       get() = pokemonDetail.primaryType

    val effectiveStats: Stats get() {
        val withItem = heldItem?.boosts?.applyTo(stats) ?: stats
        return withItem.scaledBy(RarityBoost.multiplierFor(rarity) * BattleFatigue.multiplierFor(timesUsed))
    }

    val fatigueMultiplier: Float get() = BattleFatigue.multiplierFor(timesUsed)
}

data class Gym(
    val name: String, val city: String,
    val latitude: Double, val longitude: Double,
    val typeSpecialty: String, val cardPool: List<Card>,
    val difficulty: Int = 1,
    val badgeImageUrl: String? = null,
    val region: Region = Region.KANTO
) {
    init {
        require(cardPool.size >= BOT_TEAM_SIZE) { "Gym '$name' necesita al menos $BOT_TEAM_SIZE cartas en el pool" }
        require(difficulty in 1..5) { "Gym '$name': dificultad fuera de rango (1..5)" }
    }

    fun drawBotTeam(random: Random = Random.Default): List<Card> =
        cardPool.shuffled(random).take(BOT_TEAM_SIZE)

    companion object {
        const val BOT_TEAM_SIZE = TeamRules.SIZE
    }
}

enum class WeatherCondition(
    val boostedTypes: List<String>,
    val multiplier:   Float,
    val displayName:  String
) {
    SUNNY(listOf("fire", "grass"), 1.25f, "Soleado"),
    EXTREME_SUN(listOf("fire"), 1.5f, "Sol Abrasador"),
    RAIN(listOf("water", "electric"), 1.25f, "Lluvia"),
    STORM(listOf("electric", "dragon"), 1.35f, "Tormenta"),
    SNOW(listOf("ice"), 1.25f, "Nieve"),
    SANDSTORM(listOf("rock", "ground", "steel"), 1.25f, "Tormenta de Arena"),
    FOG(listOf("ghost", "psychic", "dark"), 1.2f, "Niebla"),
    CLEAR(emptyList(), 1.0f, "Despejado")
}

data class RoundResult(
    val playerCard: Card, val botCard: Card,
    val playerScore: Float, val botScore: Float,
    val winner: Winner,
    val playerCrit: Boolean = false, val botCrit: Boolean = false,
    val playerMissed: Boolean = false,
    val playerMatchup: Float = TypeMatchup.NEUTRAL,
    val botMatchup:    Float = TypeMatchup.NEUTRAL
)

data class BattleResult(
    val winner: Winner,
    val playerCard: Card, val botCard: Card,
    val playerCards: List<Card> = emptyList(),
    val botCards:    List<Card> = emptyList(),
    val playerScore: Float, val botScore: Float,
    val weatherCondition: WeatherCondition,
    val gymName: String = "",
    val date: String = "",
    val rounds: List<RoundResult> = emptyList(),
    val coinsDelta: Int = 0,
    val gymDifficulty: Int = 1
) {
    val playerWon: Boolean get() = winner == Winner.PLAYER
    val isDraw:    Boolean get() = winner == Winner.DRAW
    val playerRoundWins: Int get() = rounds.count { it.winner == Winner.PLAYER }
    val botRoundWins:    Int get() = rounds.count { it.winner == Winner.BOT }
}

data class UserStatistics(
    val totalBattles: Int = 0, val totalWins: Int = 0, val totalLosses: Int = 0,
    val currentStreak: Int = 0, val bestStreak: Int = 0,
    val favoritePokemon: String? = null,
    val coins: Int = Economy.STARTING_COINS
) {
    val totalDraws: Int   get() = totalBattles - totalWins - totalLosses
    val winRate:    Float get() = if (totalBattles == 0) 0f else totalWins / totalBattles.toFloat()
}
