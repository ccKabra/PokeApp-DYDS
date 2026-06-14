package com.pokemonarena.domain.usecase

import com.pokemonarena.domain.entity.RogueEvolutions
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.entity.RogueRules
import com.pokemonarena.domain.entity.RogueSpecies
import kotlin.random.Random

class RogueProgression(
    private val pool:   List<RogueSpecies>,
    private val random: Random
) {

    fun drawSpecies(tier: Int, count: Int): List<RogueSpecies> =
        pool.filter { it.tier == tier }.ifEmpty { pool }.shuffled(random).take(count)

    fun spawn(species: RogueSpecies, level: Int): RoguePokemon =
        RoguePokemon.of(evolvedSpecies(species, level), level)

    fun recruitLevel(team: List<RoguePokemon>): Int =
        (team.maxOfOrNull { it.level } ?: RogueRules.BASE_LEVEL) + 1

    fun grantXp(team: List<RoguePokemon>, amount: Int): Pair<List<RoguePokemon>, List<String>> {
        val notes = mutableListOf<String>()
        val updated = team.map { member ->
            if (!member.isAlive) return@map member
            val (grown, memberNotes) = growMember(member, amount)
            notes += memberNotes
            grown
        }
        return updated to notes
    }

    fun levelUpStrongest(team: List<RoguePokemon>, levels: Int): Pair<List<RoguePokemon>, List<String>> {
        val index = team.indices.filter { team[it].isAlive }
            .maxByOrNull { team[it].level } ?: return team to listOf("No hay Pokémon en pie para entrenar.")
        val target = team[index]
        val xp = (0 until levels).sumOf { RogueRules.xpToNext(target.level + it) }
        val (grown, notes) = growMember(target, xp)
        return team.toMutableList().also { it[index] = grown } to notes
    }

    fun evolveEligible(team: List<RoguePokemon>): Pair<List<RoguePokemon>, List<String>> {
        val notes = mutableListOf<String>()
        val updated = team.map { member ->
            if (!member.isAlive) return@map member
            val nextId  = RogueEvolutions.nextStageAny(member.species.pokeId) ?: return@map member
            val species = pool.firstOrNull { it.pokeId == nextId } ?: return@map member
            notes += "¡${member.species.displayName} evolucionó a ${species.displayName}!"
            member.evolveInto(species)
        }
        return updated to notes
    }

    private fun growMember(member: RoguePokemon, amount: Int): Pair<RoguePokemon, List<String>> {
        val notes = mutableListOf<String>()
        val outcome = member.gainingXp(amount)
        if (outcome.levelsGained > 0)
            notes += "${outcome.pokemon.species.displayName} subió a Nv ${outcome.pokemon.level}."
        outcome.learnedMoves.forEach { notes += "${outcome.pokemon.species.displayName} aprendió ${it.name}." }
        var pokemon = outcome.pokemon
        val evolved = evolvedSpecies(pokemon.species, pokemon.level)
        if (evolved.pokeId != pokemon.species.pokeId) {
            notes += "¡${pokemon.species.displayName} evolucionó a ${evolved.displayName}!"
            pokemon = pokemon.evolveInto(evolved)
        }
        return pokemon to notes
    }

    private fun evolvedSpecies(species: RogueSpecies, level: Int): RogueSpecies {
        var current = species
        while (true) {
            val nextId = RogueEvolutions.nextStage(current.pokeId, level) ?: break
            current = pool.firstOrNull { it.pokeId == nextId } ?: break
        }
        return current
    }
}
