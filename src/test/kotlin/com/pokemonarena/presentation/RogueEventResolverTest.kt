package com.pokemonarena.presentation

import com.pokemonarena.FixedRandom
import com.pokemonarena.domain.entity.RogueEffect
import com.pokemonarena.domain.entity.RogueMapFactory
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.entity.Stats
import com.pokemonarena.domain.usecase.RogueProgression
import com.pokemonarena.presentation.screens.rogue.RogueEventResolver
import com.pokemonarena.presentation.screens.rogue.RogueRunSnapshot
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RogueEventResolverTest {

    private val species = RogueSpecies(1, "mon", "", listOf("normal"), Stats(50, 50, 50, 50, 50, 50), 1)

    private fun run(loot: Int = 100, inventory: List<com.pokemonarena.domain.entity.RogueItem> = emptyList()) =
        RogueRunSnapshot(
            act = 1, map = RogueMapFactory.generate(1, Random(1)),
            team = listOf(RoguePokemon.of(species)), loot = loot, inventory = inventory
        )

    private fun resolver(rng: Random) = RogueEventResolver(rng, RogueProgression(listOf(species), rng))

    @Test
    fun `loot_adjustsGold_flooredAtZero`() {
        val r = resolver(FixedRandom(0.5f))
        assertEquals(20, r.resolve(run(loot = 100), listOf(RogueEffect.Loot(-80))).run.loot)
        assertEquals(0, r.resolve(run(loot = 100), listOf(RogueEffect.Loot(-200))).run.loot)
    }

    @Test
    fun `fightEffect_signalsCombat`() {
        val result = resolver(FixedRandom(0.5f)).resolve(run(), listOf(RogueEffect.Fight))
        assertEquals(RogueEventResolver.Next.FIGHT, result.next)
    }

    @Test
    fun `giveItem_addsToTheBag_andContinues`() {
        val result = resolver(FixedRandom(0.5f)).resolve(run(), listOf(RogueEffect.GiveItem))
        assertEquals(1, result.run.inventory.size)
        assertEquals(RogueEventResolver.Next.CONTINUE, result.next)
    }

    @Test
    fun `chance_takesTheWinningBranchWhenRollIsLow`() {
        val effects = listOf(RogueEffect.Chance(0.5f,
            onWin = listOf(RogueEffect.Loot(100)),
            onLose = listOf(RogueEffect.Loot(-50))))

        val won  = resolver(FixedRandom(0.0f)).resolve(run(loot = 100), effects)
        val lost = resolver(FixedRandom(0.99f)).resolve(run(loot = 100), effects)

        assertEquals(200, won.run.loot, "roll bajo entra a la rama de éxito")
        assertEquals(50, lost.run.loot, "roll alto entra a la rama de fallo")
    }

    @Test
    fun `costLoot_blocksWhenYouCannotAfford_butPaysAndAppliesWhenYouCan`() {
        val r = resolver(FixedRandom(0.5f))
        val blocked = r.resolve(run(loot = 30), listOf(RogueEffect.CostLoot(200, listOf(RogueEffect.Heal(0.5f)))))
        assertEquals(30, blocked.run.loot, "sin oro suficiente no se cobra nada")
        assertTrue(blocked.messages.any { it.contains("No te alcanza") })

        val paid = r.resolve(run(loot = 300), listOf(RogueEffect.CostLoot(200, listOf(RogueEffect.Loot(50)))))
        assertEquals(150, paid.run.loot, "paga 200 y luego aplica el +50")
    }

    @Test
    fun `hurt_neverFaints_andHealRestoresHp`() {
        val r = resolver(FixedRandom(0.5f))
        val hurt = r.resolve(run(), listOf(RogueEffect.Hurt(1.0f))).run.team.single()
        assertTrue(hurt.isAlive, "un evento nunca debilita")

        val damaged = run().copy(team = listOf(RoguePokemon.of(species).damaged(60)))
        val healed = r.resolve(damaged, listOf(RogueEffect.Heal(0.5f))).run.team.single()
        assertTrue(healed.currentHp > damaged.team.single().currentHp)
    }
}
