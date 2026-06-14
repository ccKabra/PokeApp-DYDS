package com.pokemonarena.data.repository

import com.pokemonarena.data.local.dao.RogueLivesDao
import com.pokemonarena.data.local.dao.RogueUpgradeDao
import com.pokemonarena.domain.entity.RogueLives
import com.pokemonarena.domain.entity.RogueMetaState
import com.pokemonarena.domain.repository.RogueMetaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RogueMetaRepositoryImpl(
    private val upgradeDao: RogueUpgradeDao,
    private val livesDao:   RogueLivesDao
) : RogueMetaRepository {

    override fun getUpgrades(): Flow<RogueMetaState> =
        upgradeDao.observeAll().map { RogueMetaState(it) }

    override suspend fun setLevel(upgradeId: String, level: Int) = upgradeDao.setLevel(upgradeId, level)

    override suspend fun getLives(): RogueLives = livesDao.get(System.currentTimeMillis())

    override suspend fun saveLives(lives: RogueLives) = livesDao.save(lives)
}
