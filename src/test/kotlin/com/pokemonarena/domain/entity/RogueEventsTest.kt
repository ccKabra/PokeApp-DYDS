package com.pokemonarena.domain.entity

import kotlin.test.Test
import kotlin.test.assertTrue

class RogueEventsTest {

    @Test
    fun `catalog_coversTheRequiredEventsAndThenSome`() {
        val ids = RogueEvents.ALL.map { it.id }.toSet()
        listOf("entrenador", "altar", "comerciante", "manantial").forEach {
            assertTrue(it in ids, "falta el evento mínimo requerido: $it")
        }
        assertTrue(RogueEvents.ALL.size >= 6, "se pidieron los 4 mínimos y crear más")
    }

    @Test
    fun `everyEventOffersExactlyThreeNonEmptyOptions`() {
        RogueEvents.ALL.forEach { event ->
            assertTrue(event.options.size == 3, "${event.id} debe ofrecer 3 opciones")
            event.options.forEach { option ->
                assertTrue(option.label.isNotBlank())
                assertTrue(option.effects.isNotEmpty(), "cada opción tiene al menos un efecto")
            }
        }
    }

    @Test
    fun `eventIdsAreUnique`() {
        val ids = RogueEvents.ALL.map { it.id }
        assertTrue(ids.size == ids.toSet().size, "los ids de evento no se repiten")
    }
}
