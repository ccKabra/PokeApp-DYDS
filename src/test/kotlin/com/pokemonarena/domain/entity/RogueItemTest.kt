package com.pokemonarena.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RogueItemTest {

    private fun species() =
        RogueSpecies(1, "testmon", "", listOf("normal"), Stats(80, 90, 70, 60, 50, 85), tier = 1)

    @Test
    fun `equip_appliesStatMultipliersAndRecordsTheItem`() {
        val base = RoguePokemon.of(species())
        val gear = RogueItem("x", "Coraza", "test", defenseMult = 1.5f, hpMult = 2f)

        val equipped = base.equip(gear)

        assertEquals(gear, equipped.item)
        assertEquals((base.defense * 1.5f).toInt(), equipped.defense)
        assertEquals((base.maxHp * 2f).toInt(), equipped.maxHp)
        assertTrue(equipped.maxHp > base.maxHp)
    }

    @Test
    fun `equip_onFullHpKeepsItFull`() {
        val base = RoguePokemon.of(species())
        val equipped = base.equip(RogueItem("x", "Coraza", "test", hpMult = 2f))
        assertEquals(equipped.maxHp, equipped.currentHp, "equipar +HP a pleno deja a pleno")
    }

    @Test
    fun `equip_doesNotReviveAFaintedPokemon`() {
        val fainted = RoguePokemon.of(species()).damaged(99_999)
        val equipped = fainted.equip(RogueItem("x", "Coraza", "test", hpMult = 2f))
        assertTrue(!equipped.isAlive, "un item no revive a un debilitado")
        assertEquals(0, equipped.currentHp)
    }

    @Test
    fun `gainingXp_preservesTheEquippedItemAndItsBonus`() {
        val equipped = RoguePokemon.of(species()).equip(RogueItem("x", "Coraza", "test", attackMult = 1.5f))
        val baseAttackAtNewLevel = RoguePokemon.of(species(), level = equipped.level + 5).attack

        val leveled = equipped.gainingXp(5_000).pokemon

        assertNotNull(leveled.item, "el item no debe perderse al subir de nivel")
        assertTrue(leveled.attack > baseAttackAtNewLevel,
                   "el bonus del item se re-aplica sobre las stats del nuevo nivel")
    }

    @Test
    fun `catalog_hasItemsAndRandomReturnsOne`() {
        assertTrue(RogueItems.ALL.isNotEmpty())
        assertTrue(RogueItems.ALL.all { it.name.isNotBlank() })
    }
}
