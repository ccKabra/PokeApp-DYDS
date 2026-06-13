package com.pokemonarena.data.external

import com.pokemonarena.data.external.broker.CardsSourceWithFallback
import com.pokemonarena.data.external.broker.ExternalCardInfo
import com.pokemonarena.data.external.broker.PokemonCardsExternalSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CardsSourceWithFallbackTest {

    private val primary   = mockk<PokemonCardsExternalSource>()
    private val secondary = mockk<PokemonCardsExternalSource>()
    private val source    = CardsSourceWithFallback(primary, secondary)

    private val card = ExternalCardInfo("dex-1", "Pikachu", "s.webp", "l.webp", "Common", "Base")

    @Test
    fun `getCards_whenPrimaryHasResults_doesNotCallSecondary`() = runTest {
        coEvery { primary.getCardsByPokemonName("pikachu") } returns listOf(card)

        val result = source.getCardsByPokemonName("pikachu")

        assertEquals(listOf(card), result)
        coVerify(exactly = 0) { secondary.getCardsByPokemonName(any()) }
    }

    @Test
    fun `getCards_whenPrimaryIsEmpty_fallsBackToSecondary`() = runTest {
        coEvery { primary.getCardsByPokemonName("mew") } returns emptyList()
        coEvery { secondary.getCardsByPokemonName("mew") } returns listOf(card.copy(id = "tcg-1"))

        val result = source.getCardsByPokemonName("mew")

        assertEquals("tcg-1", result.single().id)
    }
}
