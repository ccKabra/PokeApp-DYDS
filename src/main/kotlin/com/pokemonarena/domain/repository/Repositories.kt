package com.pokemonarena.domain.repository

import com.pokemonarena.domain.entity.*
import kotlinx.coroutines.flow.Flow

interface PokemonRepository {
    suspend fun getPokemonList(limit: Int = 20, offset: Int = 0): List<Pokemon>
    suspend fun getPokemonDetail(name: String): PokemonDetail
}

interface CardRepository {
    suspend fun getCardsForPokemon(pokemonName: String): List<Card>

    fun getOwnedCards(): Flow<List<Card>>

    suspend fun purchaseCard(card: Card)

    suspend fun removeCard(cardId: String)

    fun isCardOwned(cardId: String): Flow<Boolean>

    fun getTeamCards(): Flow<List<Card>>

    suspend fun updateTeamMembership(cardId: String, inTeam: Boolean)

    suspend fun setHeldItem(cardId: String, itemId: String?)

    suspend fun registerBattleUsage(cardIds: List<String>)

    suspend fun resetBattleUsage(cardId: String)
}

interface BadgeRepository {
    fun getEarnedBadges(): Flow<Set<String>>
    suspend fun hasBadge(gymName: String): Boolean
    suspend fun awardBadge(gymName: String)
}

interface ItemRepository {
    suspend fun getCatalog(): List<Item>
    fun getInventory(): Flow<Map<String, Int>>
    suspend fun addToInventory(itemId: String)
    suspend fun removeFromInventory(itemId: String)
}

interface GymRepository {
    suspend fun getGyms(): List<Gym>
    suspend fun getGymByName(name: String): Gym?
    suspend fun getLeagues(): List<League>
}

interface WeatherRepository {
    suspend fun getWeatherCondition(lat: Double, lon: Double): WeatherCondition
}

interface RoguePoolRepository {
    suspend fun getPool(): List<RogueSpecies>
}

interface RogueMetaRepository {
    fun getUpgrades(): Flow<RogueMetaState>
    suspend fun setLevel(upgradeId: String, level: Int)
    suspend fun getLives(): RogueLives
    suspend fun saveLives(lives: RogueLives)
}

interface ProfileRepository {
    fun getProfile(): Flow<PlayerProfile?>
    suspend fun saveProfile(profile: PlayerProfile)
}

interface BattleRepository {
    suspend fun saveBattleResult(result: BattleResult)
    suspend fun saveUserStatistics(stats: UserStatistics)
    fun getBattleHistory(): Flow<List<BattleResult>>
    fun getUserStatistics(): Flow<UserStatistics>
}
