package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.Progression
import com.pokemonarena.domain.entity.Region
import com.pokemonarena.domain.repository.BadgeRepository
import com.pokemonarena.domain.repository.GymRepository
import com.pokemonarena.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.first

class GetGymsUseCase(private val repo: GymRepository) {
    suspend fun execute() = repo.getGyms()
}

class GetGymByNameUseCase(private val repo: GymRepository) {
    suspend fun execute(name: String) = repo.getGymByName(name)
}

class GetWeatherConditionUseCase(private val repo: WeatherRepository) {
    suspend fun execute(lat: Double, lon: Double) = repo.getWeatherCondition(lat, lon)
}

class GetLeaguesUseCase(private val repo: GymRepository) {
    suspend fun execute() = repo.getLeagues()
}

class GetRegionProgressUseCase(
    private val gymRepo:   GymRepository,
    private val badgeRepo: BadgeRepository
) {
    data class Progress(
        val unlockedRegions: Set<Region>,
        val maxPokedexId:    Int,
        val earnedBadges:    Set<String>
    )

    suspend fun execute(): Progress {
        val gyms     = gymRepo.getGyms()
        val leagues  = gymRepo.getLeagues()
        val earned   = badgeRepo.getEarnedBadges().first()
        val unlocked = Progression.unlockedRegions(gyms, leagues, earned)
        return Progress(unlocked, Progression.maxPokedexId(unlocked), earned)
    }
}
