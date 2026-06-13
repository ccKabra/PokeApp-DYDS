package com.pokemonarena.data.repository

import com.pokemonarena.TestFixtures
import com.pokemonarena.data.external.broker.ExternalCardInfo
import com.pokemonarena.data.external.broker.PokemonCardsExternalSource
import com.pokemonarena.data.external.broker.PokemonDetailExternalSource
import com.pokemonarena.data.external.broker.PokemonFullDetailExternalSource
import com.pokemonarena.data.local.dao.FavoriteCardDao
import com.pokemonarena.domain.entity.Pokemon
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardRepositoryImplTest {

    private val broker       = mockk<PokemonDetailExternalSource>()
    private val detailSource = mockk<PokemonFullDetailExternalSource>()
    private val cardsSource  = mockk<PokemonCardsExternalSource>()
    private val dao          = mockk<FavoriteCardDao>(relaxed = true)
    private val repo         = CardRepositoryImpl(broker, detailSource, cardsSource, dao)

    private fun cardInfo(id: String, name: String = "Venusaur") = ExternalCardInfo(
        id = id, name = name,
        imageSmall = "https://img/$id-s.png", imageLarge = "https://img/$id-l.png",
        rarity = "Rare", setName = "Base Set"
    )

    @Test
    fun `getCardsForPokemon_withMultipleTcgCards_returnsOneCardPerVersion`() = runTest {
        coEvery { detailSource.getPokemonDetailFull("venusaur") } returns TestFixtures.grassDetail
        coEvery { cardsSource.getCardsByPokemonName("venusaur") } returns
            listOf(cardInfo("base1-15"), cardInfo("base4-18"), cardInfo("ex3-28"))

        val cards = repo.getCardsForPokemon("Venusaur")

        assertEquals(3, cards.size)
        assertEquals(listOf("base1-15", "base4-18", "ex3-28"), cards.map { it.id })
        assertTrue(cards.all { it.pokemonDetail == TestFixtures.grassDetail })
        assertEquals("Base Set", cards.first().setName)
    }

    @Test
    fun `getCardsForPokemon_withoutTcgCards_fallsBackToBrokerArtwork`() = runTest {
        coEvery { detailSource.getPokemonDetailFull("venusaur") } returns TestFixtures.grassDetail
        coEvery { cardsSource.getCardsByPokemonName("venusaur") } returns emptyList()
        coEvery { broker.getPokemonByName("venusaur") } returns
            Pokemon(3, "venusaur", "https://img/art.png", listOf("grass"))

        val cards = repo.getCardsForPokemon("Venusaur")

        assertEquals(1, cards.size)
        assertEquals("https://img/art.png", cards.first().imageUrlSmall)
    }

    @Test
    fun `getCardsForPokemon_whenAllSourcesFail_returnsEmptyList`() = runTest {
        coEvery { detailSource.getPokemonDetailFull(any()) } throws RuntimeException("network")
        coEvery { cardsSource.getCardsByPokemonName(any()) } returns emptyList()
        coEvery { broker.getPokemonByName(any()) } returns null

        assertTrue(repo.getCardsForPokemon("venusaur").isEmpty())
    }
}
