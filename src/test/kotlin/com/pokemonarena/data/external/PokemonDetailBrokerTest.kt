package com.pokemonarena.data.external

import com.pokemonarena.data.external.broker.PokemonDetailBroker
import com.pokemonarena.data.external.broker.PokemonDetailExternalSource
import com.pokemonarena.domain.entity.Pokemon
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PokemonDetailBrokerTest {

    private lateinit var fakePokeApi: FakePokemonDetailSource
    private lateinit var fakeTcg:     FakePokemonDetailSource
    private lateinit var broker:      PokemonDetailBroker

    private val pokeApiPokemon = Pokemon(1, "bulbasaur", "https://official-artwork/bulbasaur.png", listOf("grass"))
    private val tcgPokemon     = Pokemon(0, "bulbasaur", "https://tcg-card/bulbasaur-card.png",    emptyList())

    @BeforeTest
    fun setup() {
        fakePokeApi = FakePokemonDetailSource()
        fakeTcg     = FakePokemonDetailSource()
        broker      = PokemonDetailBroker(fakePokeApi, fakeTcg)
    }

    @Test
    fun `getPokemonByName deberia combinar pokeapi y tcg cuando ambos responden`() = runTest {
        fakePokeApi.pokemonToReturn = pokeApiPokemon
        fakeTcg.pokemonToReturn     = tcgPokemon

        val result = broker.getPokemonByName("bulbasaur")

        assertEquals(pokeApiPokemon.id,    result?.id)
        assertEquals(pokeApiPokemon.name,  result?.name)
        assertEquals(pokeApiPokemon.types, result?.types)
        assertEquals(tcgPokemon.imageUrl,  result?.imageUrl)
    }

    @Test
    fun `getPokemonByName deberia retornar datos de pokeapi cuando solo pokeapi responde`() = runTest {
        fakePokeApi.pokemonToReturn = pokeApiPokemon
        fakeTcg.shouldFail          = true

        val result = broker.getPokemonByName("bulbasaur")

        assertEquals(pokeApiPokemon.imageUrl, result?.imageUrl)
        assertEquals(pokeApiPokemon.id,       result?.id)
    }

    @Test
    fun `getPokemonByName deberia retornar datos de tcg cuando solo tcg responde`() = runTest {
        fakePokeApi.shouldFail      = true
        fakeTcg.pokemonToReturn     = tcgPokemon

        val result = broker.getPokemonByName("bulbasaur")

        assertEquals(tcgPokemon.imageUrl, result?.imageUrl)
        assertEquals(tcgPokemon.name,     result?.name)
    }

    @Test
    fun `getPokemonByName deberia retornar null cuando ambos servicios fallan`() = runTest {
        fakePokeApi.shouldFail = true
        fakeTcg.shouldFail     = true

        val result = broker.getPokemonByName("bulbasaur")

        assertNull(result)
    }

    @Test
    fun `getPokemonByName deberia preferir la imagen de tcg cuando ambas fuentes responden`() = runTest {
        fakePokeApi.pokemonToReturn = pokeApiPokemon
        fakeTcg.pokemonToReturn     = tcgPokemon

        val result = broker.getPokemonByName("bulbasaur")

        assertEquals(tcgPokemon.imageUrl, result?.imageUrl)
    }

    @Test
    fun `getPokemonByName deberia usar imagen de pokeapi cuando tcg retorna imagen vacia`() = runTest {
        fakePokeApi.pokemonToReturn = pokeApiPokemon
        fakeTcg.pokemonToReturn     = tcgPokemon.copy(imageUrl = "")

        val result = broker.getPokemonByName("bulbasaur")

        assertEquals(pokeApiPokemon.imageUrl, result?.imageUrl)
    }

    @Test
    fun `getPokemonByName deberia retornar null cuando ambos retornan null`() = runTest {
        fakePokeApi.pokemonToReturn = null
        fakeTcg.pokemonToReturn     = null

        val result = broker.getPokemonByName("bulbasaur")

        assertNull(result)
    }

    private class FakePokemonDetailSource : PokemonDetailExternalSource {
        var shouldFail      = false
        var pokemonToReturn: Pokemon? = null

        override suspend fun getPokemonByName(name: String): Pokemon? {
            if (shouldFail) throw Exception("Source error")
            return pokemonToReturn
        }
    }
}
