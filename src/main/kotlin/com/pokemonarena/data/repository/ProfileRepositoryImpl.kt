package com.pokemonarena.data.repository

import com.pokemonarena.data.local.dao.UserProfileDao
import com.pokemonarena.data.local.dao.UserProfileRow
import com.pokemonarena.domain.entity.PlayerGender
import com.pokemonarena.domain.entity.PlayerProfile
import com.pokemonarena.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(private val dao: UserProfileDao) : ProfileRepository {

    override fun getProfile(): Flow<PlayerProfile?> = dao.observe().map { row ->
        row?.let {
            PlayerProfile(it.name, runCatching { PlayerGender.valueOf(it.gender) }
                .getOrDefault(PlayerGender.MALE))
        }
    }

    override suspend fun saveProfile(profile: PlayerProfile) =
        dao.upsert(UserProfileRow(profile.name, profile.gender.name))
}
