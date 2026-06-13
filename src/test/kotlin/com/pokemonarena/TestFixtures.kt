package com.pokemonarena

import com.pokemonarena.domain.entity.*
import kotlin.random.Random

// Random determinista para tests: nextFloat() devuelve siempre `value`.
// Con 0.99 nunca hay crítico/jackpot; con 0.0 siempre toca el mejor premio.
class FixedRandom(private val value: Float) : Random() {
    override fun nextBits(bitCount: Int): Int = (value * (1L shl bitCount)).toLong().toInt()
}

object TestFixtures {

    val balancedStats = Stats(hp = 80, attack = 80, defense = 80,
                              specialAttack = 80, specialDefense = 80, speed = 80)
    val highStats     = Stats(hp = 200, attack = 200, defense = 200,
                              specialAttack = 200, specialDefense = 200, speed = 200)
    val lowStats      = Stats(hp = 10, attack = 10, defense = 10,
                              specialAttack = 10, specialDefense = 10, speed = 10)
    val zeroStats     = Stats(0, 0, 0, 0, 0, 0)

    fun detail(
        id: Int = 1, name: String = "bulbasaur",
        types: List<String> = listOf("grass"), stats: Stats = balancedStats
    ) = PokemonDetail(id, name, "https://img/$name.png", types, 7, 69, stats,
                      listOf(name))

    val fireDetail  = detail(4,   "charmander", listOf("fire"),  balancedStats)
    val waterDetail = detail(7,   "squirtle",   listOf("water"), balancedStats)
    val grassDetail = detail(1,   "bulbasaur",  listOf("grass"), balancedStats)
    val strongFire  = detail(6,   "charizard",  listOf("fire"),  highStats)
    val weakWater   = detail(7,   "squirtle",   listOf("water"), lowStats)

    fun card(id: String = "base1-1", name: String = "Bulbasaur",
             pokemonDetail: PokemonDetail = grassDetail) =
        Card(id, name, "https://img/$id-s.png", "https://img/$id-l.png",
             "Common", "Base Set", pokemonDetail)

    val fireCard   = card("base1-4", "Charmander",  fireDetail)
    val waterCard  = card("base1-7", "Squirtle",    waterDetail)
    val grassCard  = card("base1-1", "Bulbasaur",   grassDetail)
    val strongCard = card("base1-6", "Charizard",   strongFire)
    val weakCard   = card("base1-7b","Squirtle-W",  weakWater)

    fun pokemon(id: Int = 1, name: String = "bulbasaur") =
        Pokemon(id, name, "https://img/$name.png", listOf("grass"))

    val pokemonList = (1..5).map { pokemon(it, "pokemon_$it") }

    fun battleResult(
        winner: Winner = Winner.PLAYER,
        playerScore: Float = 0.6f, botScore: Float = 0.4f,
        weather: WeatherCondition = WeatherCondition.CLEAR,
        playerCard: Card = grassCard, botCard: Card = fireCard,
        coinsDelta: Int = 0
    ) = BattleResult(
        winner = winner, playerCard = playerCard, botCard = botCard,
        playerCards = listOf(playerCard), botCards = listOf(botCard),
        playerScore = playerScore, botScore = botScore,
        weatherCondition = weather, gymName = "Test Gym",
        date = "2024-01-01T10:00:00", coinsDelta = coinsDelta
    )

    val playerWin = battleResult(Winner.PLAYER, 0.6f, 0.4f, coinsDelta = 45)
    val botWin    = battleResult(Winner.BOT,    0.3f, 0.7f, coinsDelta = -10)
    val draw      = battleResult(Winner.DRAW,   0.5f, 0.5f, coinsDelta = 0)

    fun gym(difficulty: Int = 1, botCards: List<Card> = listOf(fireCard, waterCard, grassCard)) =
        Gym("Test Gym", "Test City", 0.0, 0.0, "rock", botCards, difficulty)

    val emptyStats = UserStatistics()
    val statsWithData = UserStatistics(
        totalBattles = 10, totalWins = 6, totalLosses = 3,
        currentStreak = 2, bestStreak = 4, favoritePokemon = "bulbasaur"
    )
}
