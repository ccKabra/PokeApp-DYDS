package com.pokemonarena.domain.entity

import kotlin.random.Random

enum class RogueNodeType(val displayName: String, val description: String) {
    FIGHT("Combate", "Un Pokémon salvaje te cierra el paso. Vencelo por XP y oro."),
    CAPTURE("Captura", "Un Pokémon salvaje quiere unírsete. Sumalo a tu equipo."),
    ITEM("Objeto", "Encontrás un objeto para potenciar a uno de tus Pokémon."),
    CENTER("Centro Pokémon", "Una sala segura: cura por completo a tu equipo en pie."),
    GOLD("Cofre de Oro", "Un cofre repleto de oro que sumás al botín de la run."),
    EVENT("Evento", "Una situación inesperada: elegís una de tres opciones."),
    BOSS("Jefe", "El guardián del acto. Vencelo para avanzar.")
}

data class RogueMapNode(
    val id:   Int,
    val row:  Int,
    val col:  Int,
    val type: RogueNodeType,
    val next: List<Int> = emptyList()
)

data class RogueMap(
    val act:   Int,
    val nodes: List<RogueMapNode>
) {
    val rows: List<List<RogueMapNode>> =
        nodes.groupBy { it.row }.toSortedMap().values.map { it.sortedBy { n -> n.col } }

    fun node(id: Int): RogueMapNode = nodes.first { it.id == id }

    val entryNodeIds: List<Int> get() = rows.first().map { it.id }

    val bossNodeId: Int get() = nodes.first { it.type == RogueNodeType.BOSS }.id
}

object RogueMapFactory {

    private data class Weighted(val type: RogueNodeType, val weight: Int)

    private val WEIGHTS = listOf(
        Weighted(RogueNodeType.FIGHT, 34),
        Weighted(RogueNodeType.EVENT, 18),
        Weighted(RogueNodeType.GOLD, 13),
        Weighted(RogueNodeType.ITEM, 13),
        Weighted(RogueNodeType.CAPTURE, 13),
        Weighted(RogueNodeType.CENTER, 9)
    )

    fun generate(act: Int, random: Random): RogueMap {
        val nodes = mutableListOf<RogueMapNode>()
        var nextId = 0
        val rowsOfIds = mutableListOf<List<Int>>()

        repeat(RogueRules.ROWS_PER_ACT) { row ->
            val width = random.nextInt(RogueRules.MIN_ROW_WIDTH, RogueRules.MAX_ROW_WIDTH + 1)
            val ids = (0 until width).map { col ->
                val type = nodeTypeFor(row, col, random)
                RogueMapNode(nextId, row, col, type).also { nodes += it; nextId++ }.id
            }
            rowsOfIds += ids
        }

        val bossRow = RogueRules.ROWS_PER_ACT
        val bossId  = nextId
        nodes += RogueMapNode(bossId, bossRow, 0, RogueNodeType.BOSS)
        rowsOfIds += listOf(bossId)

        val edges = connect(rowsOfIds, nodes, random)
        val linked = nodes.map { it.copy(next = edges[it.id].orEmpty().sorted()) }
        return RogueMap(act, linked)
    }

    private fun nodeTypeFor(row: Int, col: Int, random: Random): RogueNodeType = when {
        row == 0 && col == 0                       -> RogueNodeType.FIGHT
        row == RogueRules.ROWS_PER_ACT - 1 && col == 0 -> RogueNodeType.CENTER
        row == 0                                   -> rollType(random, exclude = RogueNodeType.CENTER)
        else                                       -> rollType(random)
    }

    private fun rollType(random: Random, exclude: RogueNodeType? = null): RogueNodeType {
        val pool  = WEIGHTS.filter { it.type != exclude }
        val total = pool.sumOf { it.weight }
        var roll  = random.nextInt(total)
        for ((type, weight) in pool) {
            if (roll < weight) return type
            roll -= weight
        }
        return RogueNodeType.FIGHT
    }

    private fun connect(rows: List<List<Int>>, nodes: List<RogueMapNode>,
                        random: Random): Map<Int, MutableSet<Int>> {
        val edges = nodes.associate { it.id to mutableSetOf<Int>() }
        val nodeById = nodes.associateBy { it.id }

        fun position(id: Int, rowSize: Int): Float {
            val col = nodeById.getValue(id).col
            return if (rowSize <= 1) 0.5f else col / (rowSize - 1f)
        }

        for (r in 0 until rows.size - 1) {
            val current = rows[r]
            val nextRow = rows[r + 1]
            fun nearest(fromId: Int): Int =
                nextRow.minByOrNull {
                    kotlin.math.abs(position(it, nextRow.size) - position(fromId, current.size))
                }!!

            current.forEach { fromId ->
                val target = nearest(fromId)
                edges.getValue(fromId).add(target)
                if (nextRow.size > 1 && random.nextInt(100) < 45) {
                    val neighbors = nextRow.filter { it != target }
                    neighbors.minByOrNull {
                        kotlin.math.abs(position(it, nextRow.size) - position(fromId, current.size))
                    }?.let { edges.getValue(fromId).add(it) }
                }
            }
            nextRow.forEach { toId ->
                if (edges.values.none { toId in it }) {
                    val from = current.minByOrNull {
                        kotlin.math.abs(position(it, current.size) - position(toId, nextRow.size))
                    }!!
                    edges.getValue(from).add(toId)
                }
            }
        }
        return edges
    }
}
