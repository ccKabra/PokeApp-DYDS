package com.pokemonarena.domain.usecase

import com.pokemonarena.FixedRandom
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.entity.Stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RogueProgressionTest {

    private val oddish  = RogueSpecies(43, "oddish",  "", listOf("grass", "poison"), Stats(45, 50, 55, 75, 65, 30), 1)
    private val gloom   = RogueSpecies(44, "gloom",   "", listOf("grass", "poison"), Stats(60, 65, 70, 85, 75, 40), 2)
    private val rattata = RogueSpecies(19, "rattata", "", listOf("normal"),          Stats(30, 56, 35, 25, 35, 72), 1)
    private val pool    = listOf(oddish, gloom, rattata)

    private val progression = RogueProgression(pool, FixedRandom(0.5f))

    @Test
    fun `drawSpecies_filtersByTier_andFallsBackToWholePoolWhenEmpty`() {
        assertEquals(listOf(gloom), progression.drawSpecies(tier = 2, count = 5))
        assertEquals(1, progression.drawSpecies(tier = 99, count = 1).size, "sin ese tier usa todo el pool")
    }

    @Test
    fun `grantXp_skipsFainted_andLevelsTheRest`() {
        val alive   = RoguePokemon.of(rattata, level = 5)
        val fainted = RoguePokemon.of(oddish, level = 5).damaged(99_999)

        val (team, notes) = progression.grantXp(listOf(alive, fainted), amount = 5_000)

        assertTrue(team[0].level > alive.level, "el que está en pie sube de nivel")
        assertEquals(0, team[1].currentHp, "el debilitado no recibe XP ni revive")
        assertTrue(notes.isNotEmpty())
    }

    @Test
    fun `grantXp_evolvesWhenReachingTheLevelThreshold`() {
        val (team, _) = progression.grantXp(listOf(RoguePokemon.of(oddish, level = 11)), amount = 5_000)
        assertEquals(gloom.pokeId, team.single().species.pokeId, "al pasar el umbral evoluciona")
    }

    @Test
    fun `evolveEligible_evolvesOnlyThoseWithATargetInThePool`() {
        val team = listOf(RoguePokemon.of(oddish, 5), RoguePokemon.of(rattata, 5))

        val (evolved, notes) = progression.evolveEligible(team)

        assertEquals(gloom.pokeId, evolved[0].species.pokeId, "oddish evoluciona aunque sea nivel bajo")
        assertEquals(rattata.pokeId, evolved[1].species.pokeId, "raticate no está en el pool: rattata queda igual")
        assertEquals(1, notes.size)
    }

    @Test
    fun `recruitLevel_isTeamMaxPlusOne`() {
        val team = listOf(RoguePokemon.of(rattata, 4), RoguePokemon.of(oddish, 9))
        assertEquals(10, progression.recruitLevel(team))
    }

    @Test
    fun `spawn_arrivesEvolvedForItsLevel`() {
        assertEquals(gloom.pokeId, progression.spawn(oddish, level = 20).species.pokeId)
    }
}
