package com.pokemonarena.data.repository

import com.pokemonarena.data.local.dao.GymBadgeDao
import com.pokemonarena.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow

class BadgeRepositoryImpl(private val dao: GymBadgeDao) : BadgeRepository {
    override fun getEarnedBadges(): Flow<Set<String>> = dao.observeAll()
    override suspend fun hasBadge(gymName: String): Boolean = dao.contains(gymName)
    override suspend fun awardBadge(gymName: String) = dao.insert(gymName)
}
