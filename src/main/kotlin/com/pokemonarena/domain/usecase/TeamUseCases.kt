package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.TeamRules
import com.pokemonarena.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow

class GetTeamUseCase(private val repo: CardRepository) {
    fun execute(): Flow<List<Card>> = repo.getTeamCards()
}

class UpdateTeamUseCase(private val repo: CardRepository) {
    suspend fun execute(cardId: String, inTeam: Boolean, currentTeam: List<Card>): Boolean {
        if (inTeam && currentTeam.size >= TeamRules.SIZE) return false
        repo.updateTeamMembership(cardId, inTeam)
        return true
    }
}
