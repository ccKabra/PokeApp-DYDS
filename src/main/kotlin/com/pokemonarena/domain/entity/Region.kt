package com.pokemonarena.domain.entity

import kotlin.random.Random

enum class Region(val displayName: String, val generation: Int, val maxPokedexId: Int) {
    KANTO("Kanto", 1, 151),
    JOHTO("Johto", 2, 251),
    HOENN("Hoenn", 3, 386);

    val previous: Region? get() = entries.getOrNull(ordinal - 1)
}

object RegionDifficulty {
    const val BOOST_PER_REGION = 0.10f

    fun botStatMultiplier(region: Region): Float = 1f + BOOST_PER_REGION * region.ordinal
}

data class LeagueOpponent(
    val name: String,
    val specialty: String,
    val cardPool: List<Card>,
    val imageUrl: String? = null
) {
    init {
        require(cardPool.size >= Gym.BOT_TEAM_SIZE) { "El rival $name necesita al menos ${Gym.BOT_TEAM_SIZE} cartas" }
    }

    fun drawTeam(random: Random = Random.Default): List<Card> =
        cardPool.shuffled(random).take(Gym.BOT_TEAM_SIZE)
}

data class League(
    val region: Region,
    val opponents: List<LeagueOpponent>,
    val difficulty: Int = 5
) {
    val name: String get() = "Liga Pokémon de ${region.displayName}"
}

object Progression {

    fun gymsOf(region: Region, gyms: List<Gym>): List<Gym> = gyms.filter { it.region == region }

    fun hasAllGymBadges(region: Region, gyms: List<Gym>, earned: Set<String>): Boolean =
        gymsOf(region, gyms).let { it.isNotEmpty() && it.all { gym -> gym.name in earned } }

    fun isRegionComplete(region: Region, gyms: List<Gym>, leagues: List<League>, earned: Set<String>): Boolean {
        val league = leagues.firstOrNull { it.region == region } ?: return false
        return hasAllGymBadges(region, gyms, earned) && league.name in earned
    }

    fun unlockedRegions(gyms: List<Gym>, leagues: List<League>, earned: Set<String>): Set<Region> {
        val unlocked = mutableSetOf(Region.KANTO)
        for (region in Region.entries.drop(1)) {
            val previous = region.previous ?: continue
            if (isRegionComplete(previous, gyms, leagues, earned)) unlocked += region else break
        }
        return unlocked
    }

    fun maxPokedexId(unlocked: Set<Region>): Int = unlocked.maxOf { it.maxPokedexId }
}
