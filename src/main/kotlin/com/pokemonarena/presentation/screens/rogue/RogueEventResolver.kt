package com.pokemonarena.presentation.screens.rogue

import com.pokemonarena.domain.entity.RogueEffect
import com.pokemonarena.domain.entity.RogueItems
import com.pokemonarena.domain.entity.RoguePokemon
import com.pokemonarena.domain.usecase.RogueProgression
import kotlin.random.Random

class RogueEventResolver(
    private val random:      Random,
    private val progression: RogueProgression
) {
    enum class Next { CONTINUE, FIGHT }

    data class Resolution(val run: RogueRunSnapshot, val messages: List<String>, val next: Next)

    fun resolve(run: RogueRunSnapshot, effects: List<RogueEffect>): Resolution {
        val messages = mutableListOf<String>()
        val (finalRun, next) = applyAll(run, effects, messages)
        return Resolution(finalRun, messages, next)
    }

    private fun applyAll(run: RogueRunSnapshot, effects: List<RogueEffect>,
                         messages: MutableList<String>): Pair<RogueRunSnapshot, Next> {
        var current = run
        for (effect in effects) {
            val (updated, next) = applyEffect(current, effect, messages)
            current = updated
            if (next != Next.CONTINUE) return current to next
        }
        return current to Next.CONTINUE
    }

    private fun applyEffect(run: RogueRunSnapshot, effect: RogueEffect,
                            messages: MutableList<String>): Pair<RogueRunSnapshot, Next> = when (effect) {
        is RogueEffect.Loot -> {
            val newLoot = (run.loot + effect.delta).coerceAtLeast(0)
            messages += if (effect.delta >= 0) "+${effect.delta} de oro." else "−${-effect.delta} de oro."
            run.copy(loot = newLoot) to Next.CONTINUE
        }
        is RogueEffect.Heal -> {
            messages += "Tu equipo recuperó HP."
            run.copy(team = run.team.map { it.healedBy(effect.fraction) }) to Next.CONTINUE
        }
        is RogueEffect.Hurt -> {
            messages += "Tu equipo sufrió algo de daño."
            run.copy(team = run.team.map { hurtNonLethal(it, effect.fraction) }) to Next.CONTINUE
        }
        is RogueEffect.MaxHp -> {
            messages += "+HP máximo permanente para tu equipo."
            run.copy(team = run.team.map { if (it.isAlive) it.withMaxHpBoost(effect.factor) else it }) to Next.CONTINUE
        }
        is RogueEffect.Levels -> {
            val (team, notes) = progression.levelUpStrongest(run.team, effect.count)
            messages += notes
            run.copy(team = team) to Next.CONTINUE
        }
        RogueEffect.Evolve -> {
            val (team, notes) = progression.evolveEligible(run.team)
            messages += notes.ifEmpty { listOf("Ninguno de tus Pokémon pudo evolucionar todavía.") }
            run.copy(team = team) to Next.CONTINUE
        }
        RogueEffect.GiveItem -> {
            val item = RogueItems.random(random)
            messages += "¡Conseguiste ${item.name} para tu mochila!"
            run.copy(inventory = run.inventory + item) to Next.CONTINUE
        }
        RogueEffect.Fight -> { messages += "¡Te sale al paso un combate!"; run to Next.FIGHT }
        is RogueEffect.Chance -> {
            val branch = if (random.nextFloat() < effect.prob) effect.onWin else effect.onLose
            applyAll(run, branch, messages)
        }
        is RogueEffect.CostLoot ->
            if (run.loot < effect.amount) {
                messages += "No te alcanza el oro."
                run to Next.CONTINUE
            } else {
                messages += "Pagaste ${effect.amount} de oro."
                applyAll(run.copy(loot = run.loot - effect.amount), effect.then, messages)
            }
    }

    private fun hurtNonLethal(mon: RoguePokemon, fraction: Float): RoguePokemon {
        if (!mon.isAlive) return mon
        val damage = (mon.currentHp * fraction).toInt()
        return mon.copy(currentHp = (mon.currentHp - damage).coerceAtLeast(1))
    }
}
