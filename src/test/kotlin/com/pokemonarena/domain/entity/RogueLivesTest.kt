package com.pokemonarena.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RogueLivesTest {

    private val interval = RogueLives.REFILL_MS

    @Test
    fun `regenerated_addsOneLifePerInterval_cappedAtMax`() {
        val empty = RogueLives(0, lastRefillMs = 0L)
        assertEquals(0, empty.regenerated(interval - 1).lives, "antes del intervalo no recupera")
        assertEquals(1, empty.regenerated(interval).lives)
        assertEquals(2, empty.regenerated(interval * 2).lives)
        assertEquals(RogueLives.MAX, empty.regenerated(interval * 10).lives, "nunca supera el tope")
    }

    @Test
    fun `regenerated_fullStaysFull`() {
        val full = RogueLives.full(0L)
        assertEquals(RogueLives.MAX, full.regenerated(interval * 5).lives)
    }

    @Test
    fun `consume_takesOneAndStartsTheClockWhenWasFull`() {
        val full = RogueLives.full(0L)
        val after = full.consume(1_000L)!!
        assertEquals(RogueLives.MAX - 1, after.lives)
        assertEquals(1_000L, after.lastRefillMs, "al salir de lleno arranca el reloj de recarga")
    }

    @Test
    fun `consume_belowMax_keepsTheRefillProgress`() {
        val one = RogueLives(1, lastRefillMs = 0L)
        val after = one.consume(interval / 2)!!
        assertEquals(0, after.lives)
        assertEquals(0L, after.lastRefillMs, "consumir sin estar lleno no reinicia el progreso de recarga")
    }

    @Test
    fun `consume_withNoLives_returnsNull`() {
        assertNull(RogueLives(0, lastRefillMs = 0L).consume(interval - 1))
    }

    @Test
    fun `msUntilNextLife_isZeroWhenFull_andWithinIntervalOtherwise`() {
        assertEquals(0L, RogueLives.full(0L).msUntilNextLife(interval * 3))
        val pending = RogueLives(0, lastRefillMs = 0L).msUntilNextLife(interval / 2)
        assertTrue(pending in 1..interval, "el conteo a la próxima vida cae dentro de un intervalo")
    }
}
