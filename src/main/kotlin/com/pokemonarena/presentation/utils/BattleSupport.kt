package com.pokemonarena.presentation.utils

import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.usecase.DropHeldItemUseCase

fun <T> List<T>.swappedAdjacent(index: Int, up: Boolean): List<T>? {
    val target = if (up) index - 1 else index + 1
    if (index !in indices || target !in indices) return null
    return toMutableList().apply {
        val tmp = this[index]; this[index] = this[target]; this[target] = tmp
    }
}

data class ItemDropOutcome(val team: List<Card>, val notice: String?)

suspend fun DropHeldItemUseCase.resolveDropFor(team: List<Card>): ItemDropOutcome {
    val dropped = runCatching { execute(team) }.getOrNull()
        ?: return ItemDropOutcome(team, null)
    return ItemDropOutcome(
        team   = team.map { if (it.id == dropped.id) it.copy(heldItem = null) else it },
        notice = "¡A ${dropped.name} se le ha caído ${dropped.heldItem?.name} al entrar a la pokébola!"
    )
}
