package com.pokemonarena.domain.entity

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RogueMapTest {

    private fun maps() = (1..20).map { seed -> RogueMapFactory.generate(act = 1, random = Random(seed.toLong())) }

    @Test
    fun `generate_hasNormalRowsPlusASingleBossRow`() {
        maps().forEach { map ->
            assertEquals(RogueRules.ROWS_PER_ACT + 1, map.rows.size)
            val bossRow = map.rows.last()
            assertEquals(1, bossRow.size, "la última fila es un único jefe")
            assertEquals(RogueNodeType.BOSS, bossRow.single().type)
        }
    }

    @Test
    fun `generate_firstRowAlwaysOffersACombat`() {
        maps().forEach { map ->
            assertTrue(map.rows.first().any { it.type == RogueNodeType.FIGHT },
                       "el acto siempre puede abrir con un combate")
        }
    }

    @Test
    fun `generate_hasNoDeadEnds_everyNonFinalNodeHasAnExit`() {
        maps().forEach { map ->
            val lastRow = map.rows.size - 1
            map.nodes.filter { it.row < lastRow }.forEach { node ->
                assertTrue(node.next.isNotEmpty(), "ningún nodo intermedio puede ser un callejón sin salida")
            }
        }
    }

    @Test
    fun `generate_everyNodeBeyondEntryIsReachable`() {
        maps().forEach { map ->
            val incoming = map.nodes.flatMap { it.next }.toSet()
            map.nodes.filter { it.row > 0 }.forEach { node ->
                assertTrue(node.id in incoming, "todo nodo (salvo la entrada) debe tener al menos una entrada")
            }
        }
    }

    @Test
    fun `entryNodeIds_matchTheFirstRow`() {
        val map = RogueMapFactory.generate(act = 1, random = Random(7))
        assertEquals(map.rows.first().map { it.id }.toSet(), map.entryNodeIds.toSet())
    }
}
