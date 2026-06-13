package com.pokemonarena.data.repository

import com.pokemonarena.domain.entity.Region
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GymRepositoryImplTest {

    private val repo = GymRepositoryImpl()

    @Test
    fun `getGyms_returnsEightGymsPerRegion`() = runTest {
        val gyms = repo.getGyms()
        assertEquals(24, gyms.size)
        Region.entries.forEach { region ->
            assertEquals(8, gyms.count { it.region == region },
                "La región ${region.displayName} debe tener 8 gimnasios")
        }
    }

    @Test
    fun `getGyms_exposesFullPoolAndDrawsTeamsOfThree`() = runTest {
        repo.getGyms().forEach { gym ->
            assertTrue(gym.cardPool.size >= 3,
                "Gym '${gym.name}' debe exponer su pool completo (≥3 cartas)")
            assertEquals(3, gym.drawBotTeam().size,
                "Gym '${gym.name}' debe armar equipos de exactamente 3 cartas")
        }
    }

    @Test
    fun `getGyms_coordinatesAreDifferentForEachGym`() = runTest {
        val gyms   = repo.getGyms()
        val coords = gyms.map { "${it.latitude},${it.longitude}" }.toSet()
        assertEquals(gyms.size, coords.size, "Todos los gimnasios deben tener coordenadas únicas")
    }

    @Test
    fun `getGyms_gymNamesAreUnique`() = runTest {
        val gyms = repo.getGyms()
        assertEquals(gyms.size, gyms.map { it.name }.toSet().size)
    }

    @Test
    fun `getGyms_poolsUseOnlyPokemonOfTheRegionGeneration`() = runTest {
        val ranges = mapOf(
            Region.KANTO to 1..151,
            Region.JOHTO to 152..251,
            Region.HOENN to 252..386
        )
        repo.getGyms().forEach { gym ->
            val range = ranges.getValue(gym.region)
            gym.cardPool.forEach { card ->
                assertTrue(card.pokemonDetail.id in 1..range.last,
                    "${card.name} (#${card.pokemonDetail.id}) en ${gym.name} excede la generación de ${gym.region.displayName}")
            }
        }
    }

    @Test
    fun `getGyms_everyGymHasAUniqueBadgeSprite`() = runTest {
        val gyms = repo.getGyms()
        assertTrue(gyms.all { it.badgeImageUrl != null })
        assertEquals(gyms.size, gyms.mapNotNull { it.badgeImageUrl }.toSet().size,
            "Cada gimnasio debe tener su propio sprite de medalla")
    }

    @Test
    fun `getGyms_allPoolCardsHoldItemsAndNonZeroStats`() = runTest {
        repo.getGyms().forEach { gym ->
            gym.cardPool.forEach { card ->
                assertTrue(card.heldItem != null, "${card.name} en ${gym.name} no tiene item")
                assertTrue(card.stats.total > 0, "${card.name} en ${gym.name} tiene stats en 0")
            }
        }
    }

    @Test
    fun `getLeagues_oneLeaguePerRegionWithFourOpponents`() = runTest {
        val leagues = repo.getLeagues()
        assertEquals(Region.entries.size, leagues.size)
        assertEquals(Region.entries.toSet(), leagues.map { it.region }.toSet())
        leagues.forEach { league ->
            assertEquals(4, league.opponents.size,
                "${league.name} debe tener 4 rivales")
            league.opponents.forEach { opp ->
                assertTrue(opp.cardPool.size >= 3, "${opp.name} necesita pool de ≥3")
                assertEquals(3, opp.drawTeam().size)
                assertTrue(opp.cardPool.all { it.heldItem != null },
                    "Todas las cartas de ${opp.name} deben tener item")
            }
        }
    }
}
