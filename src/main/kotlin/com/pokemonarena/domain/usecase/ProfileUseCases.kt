package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.PlayerProfile
import com.pokemonarena.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

class GetPlayerProfileUseCase(private val repo: ProfileRepository) {
    fun execute(): Flow<PlayerProfile?> = repo.getProfile()
}

class SavePlayerProfileUseCase(private val repo: ProfileRepository) {
    suspend fun execute(profile: PlayerProfile) {
        require(profile.name.isNotBlank()) { "El nombre no puede estar vacío" }
        repo.saveProfile(profile.copy(name = profile.name.trim()))
    }
}
