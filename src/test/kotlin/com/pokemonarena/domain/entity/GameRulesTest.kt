package com.pokemonarena.domain.entity

import com.pokemonarena.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardPricingTest {

    @Test
    fun `priceOf_growsWithStats`() {
        val cheap     = TestFixtures.card("a", pokemonDetail = TestFixtures.detail(stats = TestFixtures.lowStats))
        val expensive = TestFixtures.card("b", pokemonDetail = TestFixtures.detail(stats = TestFixtures.highStats))
        assertTrue(CardPricing.priceOf(expensive) > CardPricing.priceOf(cheap))
    }

    @Test
    fun `priceOf_growsWithRarity`() {
        val common = TestFixtures.card("a").copy(rarity = "Common")
        val holo   = TestFixtures.card("b").copy(rarity = "Rare Holo")
        val gx     = TestFixtures.card("c").copy(rarity = "Rare Holo GX")
        assertTrue(CardPricing.priceOf(holo) > CardPricing.priceOf(common))
        assertTrue(CardPricing.priceOf(gx) > CardPricing.priceOf(holo))
    }

    @Test
    fun `priceOf_hasMinimumPrice`() {
        val junk = TestFixtures.card("a", pokemonDetail = TestFixtures.detail(stats = TestFixtures.zeroStats))
        assertEquals(100, CardPricing.priceOf(junk))
    }

    @Test
    fun `priceOf_isRoundedToMultiplesOfFive`() {
        val price = CardPricing.priceOf(TestFixtures.fireCard)
        assertEquals(0, price % 5)
    }

    @Test
    fun `rarityMultiplier_mapsKnownRarities`() {
        assertEquals(1.0f,  CardPricing.rarityMultiplier(null))
        assertEquals(1.0f,  CardPricing.rarityMultiplier("Common"))
        assertEquals(1.15f, CardPricing.rarityMultiplier("Uncommon"))
        assertEquals(1.35f, CardPricing.rarityMultiplier("Rare"))
        assertEquals(1.6f,  CardPricing.rarityMultiplier("Rare Holo"))
        assertEquals(2.0f,  CardPricing.rarityMultiplier("Rare Holo GX"))
        assertEquals(2.2f,  CardPricing.rarityMultiplier("Rare Ultra"))
        assertEquals(2.6f,  CardPricing.rarityMultiplier("Rare Secret"))
    }

    @Test
    fun `sellValueOf_isAboutHalfThePriceAndNeverBelowMinimum`() {
        val card = TestFixtures.strongCard
        assertTrue(CardPricing.sellValueOf(card) <= CardPricing.priceOf(card) / 2 + 5)
        val junk = TestFixtures.card("j", pokemonDetail = TestFixtures.detail(stats = TestFixtures.zeroStats))
        assertTrue(CardPricing.sellValueOf(junk) >= 50)
    }

    @Test
    fun `sellValueOf_decreasesWithFatigue`() {
        val fresh = TestFixtures.strongCard
        val tired = fresh.copy(timesUsed = 10)
        assertTrue(CardPricing.sellValueOf(tired) < CardPricing.sellValueOf(fresh))
    }

    @Test
    fun `startingCoins_canAffordAThreeCardStarterTeamButNotALegendary`() {
        val starter = TestFixtures.card("s", pokemonDetail =
            TestFixtures.detail(stats = Stats(45, 49, 49, 65, 65, 45))).copy(rarity = "Common")
        val legendary = TestFixtures.card("l", pokemonDetail =
            TestFixtures.detail(stats = Stats(106, 110, 90, 154, 90, 130))).copy(rarity = "Rare Holo GX")

        assertTrue(CardPricing.priceOf(starter) * 3 <= Economy.STARTING_COINS,
            "3 iniciales deben ser comprables con ${Economy.STARTING_COINS} monedas")
        assertTrue(CardPricing.priceOf(legendary) > Economy.STARTING_COINS,
            "un legendario no debe ser comprable con las monedas iniciales")
    }
}

class BattleRewardsTest {

    @Test
    fun `coinsFor_winPaysMoreInHarderGyms`() {
        val easy = BattleRewards.coinsFor(Winner.PLAYER, 1f, 0.9f, gymDifficulty = 1)
        val hard = BattleRewards.coinsFor(Winner.PLAYER, 1f, 0.9f, gymDifficulty = 5)
        assertTrue(hard > easy)
    }

    @Test
    fun `coinsFor_stompingAWeakGymPaysLessThanACloseWin`() {
        val close = BattleRewards.coinsFor(Winner.PLAYER, 1.0f, 0.95f, gymDifficulty = 1)
        val stomp = BattleRewards.coinsFor(Winner.PLAYER, 1.0f, 0.10f, gymDifficulty = 1)
        assertTrue(stomp < close, "aplastar debe pagar menos que una victoria ajustada")
    }

    @Test
    fun `coinsFor_lossIsNegativeAndScalesWithDifficulty`() {
        val easyLoss = BattleRewards.coinsFor(Winner.BOT, 0.5f, 1f, gymDifficulty = 1)
        val hardLoss = BattleRewards.coinsFor(Winner.BOT, 0.5f, 1f, gymDifficulty = 5)
        assertTrue(easyLoss < 0)
        assertTrue(hardLoss < easyLoss)
    }

    @Test
    fun `coinsFor_drawPaysASmallConsolation`() {
        assertEquals(30, BattleRewards.coinsFor(Winner.DRAW, 1f, 1f, gymDifficulty = 3))
    }

    @Test
    fun `maxRewardFor_matchesAPerfectlyCloseWin`() {
        assertEquals(BattleRewards.maxRewardFor(3),
                     BattleRewards.coinsFor(Winner.PLAYER, 1f, 1f, gymDifficulty = 3))
    }
}

class TypeMatchupTest {

    @Test
    fun `multiplier_fireBeatsGrass`() =
        assertEquals(TypeMatchup.ADVANTAGE, TypeMatchup.multiplier("fire", "grass"))

    @Test
    fun `multiplier_grassSuffersAgainstFire`() =
        assertEquals(TypeMatchup.DISADVANTAGE, TypeMatchup.multiplier("grass", "fire"))

    @Test
    fun `multiplier_waterBeatsFireButNotGrass`() {
        assertEquals(TypeMatchup.ADVANTAGE,    TypeMatchup.multiplier("water", "fire"))
        assertEquals(TypeMatchup.DISADVANTAGE, TypeMatchup.multiplier("water", "grass"))
    }

    @Test
    fun `multiplier_unknownOrNeutralPairsReturnNeutral`() {
        assertEquals(TypeMatchup.NEUTRAL, TypeMatchup.multiplier("normal", "fire"))
        assertEquals(TypeMatchup.NEUTRAL, TypeMatchup.multiplier("legendary", "fire"))
    }

    @Test
    fun `multiplier_isAsymmetric`() {
        // La ventaja de A sobre B no implica la inversa: cada dirección se evalúa sola.
        assertEquals(TypeMatchup.ADVANTAGE,    TypeMatchup.multiplier("electric", "water"))
        assertEquals(TypeMatchup.DISADVANTAGE, TypeMatchup.multiplier("water", "water"))
    }
}
