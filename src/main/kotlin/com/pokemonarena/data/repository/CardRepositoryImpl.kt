package com.pokemonarena.data.repository

import com.pokemonarena.data.external.broker.PokemonCardsExternalSource
import com.pokemonarena.data.external.broker.PokemonDetailExternalSource
import com.pokemonarena.data.external.broker.PokemonFullDetailExternalSource
import com.pokemonarena.data.local.dao.FavoriteCardDao
import com.pokemonarena.data.local.dao.FavoriteCardRow
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.ItemCatalog
import com.pokemonarena.domain.entity.PokemonDetail
import com.pokemonarena.domain.entity.Stats
import com.pokemonarena.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardRepositoryImpl(
    private val broker:       PokemonDetailExternalSource,
    private val detailSource: PokemonFullDetailExternalSource,
    private val cardsSource:  PokemonCardsExternalSource,
    private val dao:          FavoriteCardDao
) : CardRepository {

    override suspend fun getCardsForPokemon(pokemonName: String): List<Card> {
        val name   = pokemonName.lowercase()
        val detail = runCatching { detailSource.getPokemonDetailFull(name) }
            .getOrElse { stubDetail(pokemonName) }

        val tcgCards = runCatching { cardsSource.getCardsByPokemonName(name) }.getOrDefault(emptyList())
        if (tcgCards.isNotEmpty()) return tcgCards.map { info ->
            Card(info.id, info.name, info.imageSmall, info.imageLarge, info.rarity, info.setName, detail)
        }

        val pokemon = runCatching { broker.getPokemonByName(name) }.getOrNull()
        return listOfNotNull(pokemon).map { p ->
            Card(p.name, p.name, p.imageUrl, p.imageUrl, null, "Artwork oficial", detail)
        }
    }

    override fun getOwnedCards(): Flow<List<Card>> = dao.observeAll().map { rows -> rows.map { it.toCard() } }

    override suspend fun purchaseCard(card: Card) {
        dao.insert(FavoriteCardRow(
            cardId = card.id, pokemonName = card.pokemonDetail.name,
            imageSmall = card.imageUrlSmall, rarity = card.rarity, setName = card.setName,
            statHp = card.stats.hp, statAtk = card.stats.attack, statDef = card.stats.defense,
            statSpAtk = card.stats.specialAttack, statSpDef = card.stats.specialDefense,
            statSpd = card.stats.speed, primaryType = card.primaryType
        ))
    }

    override suspend fun removeCard(cardId: String) = dao.delete(cardId)
    override fun isCardOwned(cardId: String): Flow<Boolean> = dao.observeIsOwned(cardId)
    override fun getTeamCards(): Flow<List<Card>> = dao.observeTeam().map { rows -> rows.map { it.toCard() } }
    override suspend fun updateTeamMembership(cardId: String, inTeam: Boolean) = dao.updateTeam(cardId, inTeam)
    override suspend fun setHeldItem(cardId: String, itemId: String?) = dao.updateHeldItem(cardId, itemId)
    override suspend fun registerBattleUsage(cardIds: List<String>) = dao.incrementTimesUsed(cardIds)
    override suspend fun resetBattleUsage(cardId: String) = dao.resetTimesUsed(cardId)

    private fun FavoriteCardRow.toCard(): Card {
        val stats  = Stats(statHp, statAtk, statDef, statSpAtk, statSpDef, statSpd)
        val detail = PokemonDetail(0, pokemonName, imageSmall, listOf(primaryType), 0, 0, stats, listOf(pokemonName))
        return Card(cardId, pokemonName.replaceFirstChar { it.uppercase() }, imageSmall, imageSmall, rarity, setName, detail,
                    heldItem = heldItemId?.let { ItemCatalog.byId(it) }, timesUsed = timesUsed)
    }

    private fun stubDetail(name: String) =
        PokemonDetail(0, name.lowercase(), "", emptyList(), 0, 0, Stats(0, 0, 0, 0, 0, 0), emptyList())
}
