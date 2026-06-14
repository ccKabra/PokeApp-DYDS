package com.pokemonarena.domain.entity

data class RogueLives(val lives: Int, val lastRefillMs: Long) {

    val isFull: Boolean get() = lives >= MAX

    fun regenerated(now: Long): RogueLives {
        if (lives >= MAX) return this
        val elapsed = now - lastRefillMs
        if (elapsed < REFILL_MS) return this
        val gained    = (elapsed / REFILL_MS).toInt()
        val newLives  = (lives + gained).coerceAtMost(MAX)
        val newRefill = if (newLives >= MAX) now else lastRefillMs + gained.toLong() * REFILL_MS
        return RogueLives(newLives, newRefill)
    }

    fun msUntilNextLife(now: Long): Long {
        val r = regenerated(now)
        if (r.isFull) return 0
        return (r.lastRefillMs + REFILL_MS - now).coerceAtLeast(0)
    }

    fun consume(now: Long): RogueLives? {
        val r = regenerated(now)
        if (r.lives <= 0) return null
        return RogueLives(r.lives - 1, if (r.isFull) now else r.lastRefillMs)
    }

    companion object {
        const val MAX       = 3
        const val REFILL_MS = 15 * 60 * 1000L
        fun full(now: Long) = RogueLives(MAX, now)
    }
}
