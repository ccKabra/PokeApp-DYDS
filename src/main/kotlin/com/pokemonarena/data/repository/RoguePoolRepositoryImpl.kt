package com.pokemonarena.data.repository

import com.pokemonarena.data.external.source_a.artworkUrl
import com.pokemonarena.domain.entity.RogueSpecies
import com.pokemonarena.domain.entity.Stats
import com.pokemonarena.domain.repository.RoguePoolRepository

class RoguePoolRepositoryImpl : RoguePoolRepository {

    override suspend fun getPool(): List<RogueSpecies> = POOL

    private companion object {

        fun species(pokeId: Int, name: String, types: List<String>, stats: Stats, tier: Int) =
            RogueSpecies(pokeId, name, artworkUrl(pokeId), types, stats, tier)

        val POOL: List<RogueSpecies> = listOf(
            species(16,  "pidgey",     listOf("normal", "flying"),  Stats(40, 45, 40, 35, 35, 56),    1),
            species(19,  "rattata",    listOf("normal"),            Stats(30, 56, 35, 25, 35, 72),    1),
            species(27,  "sandshrew",  listOf("ground"),            Stats(50, 75, 85, 20, 30, 40),    1),
            species(43,  "oddish",     listOf("grass", "poison"),   Stats(45, 50, 55, 75, 65, 30),    1),
            species(58,  "growlithe",  listOf("fire"),              Stats(55, 70, 45, 70, 50, 60),    1),
            species(60,  "poliwag",    listOf("water"),             Stats(40, 50, 40, 40, 40, 90),    1),
            species(63,  "abra",       listOf("psychic"),           Stats(25, 20, 15, 105, 55, 90),   1),
            species(66,  "machop",     listOf("fighting"),          Stats(70, 80, 50, 35, 35, 35),    1),
            species(74,  "geodude",    listOf("rock", "ground"),    Stats(40, 80, 100, 30, 30, 20),   1),
            species(81,  "magnemite",  listOf("electric", "steel"), Stats(25, 35, 70, 95, 55, 45),    1),
            species(92,  "gastly",     listOf("ghost", "poison"),   Stats(30, 35, 30, 100, 35, 80),   1),
            species(120, "staryu",     listOf("water"),             Stats(30, 45, 55, 70, 55, 85),    1),
            species(133, "eevee",      listOf("normal"),            Stats(55, 55, 50, 45, 65, 55),    1),

            species(2,   "ivysaur",    listOf("grass", "poison"),   Stats(60, 62, 63, 80, 80, 60),    2),
            species(5,   "charmeleon", listOf("fire"),              Stats(58, 64, 58, 80, 65, 80),    2),
            species(8,   "wartortle",  listOf("water"),             Stats(59, 63, 80, 65, 80, 58),    2),
            species(17,  "pidgeotto",  listOf("normal", "flying"),  Stats(63, 60, 55, 50, 50, 71),    2),
            species(44,  "gloom",      listOf("grass", "poison"),   Stats(60, 65, 70, 85, 75, 40),    2),
            species(61,  "poliwhirl",  listOf("water"),             Stats(65, 65, 65, 50, 50, 90),    2),
            species(64,  "kadabra",    listOf("psychic"),           Stats(40, 35, 30, 120, 70, 105),  2),
            species(67,  "machoke",    listOf("fighting"),          Stats(80, 100, 70, 50, 60, 45),   2),
            species(75,  "graveler",   listOf("rock", "ground"),    Stats(55, 95, 115, 45, 45, 35),   2),
            species(82,  "magneton",   listOf("electric", "steel"), Stats(50, 60, 95, 120, 70, 70),   2),
            species(93,  "haunter",    listOf("ghost", "poison"),   Stats(45, 50, 45, 115, 55, 95),   2),

            species(3,   "venusaur",   listOf("grass", "poison"),   Stats(80, 82, 83, 100, 100, 80),  3),
            species(6,   "charizard",  listOf("fire", "flying"),    Stats(78, 84, 78, 109, 85, 100),  3),
            species(9,   "blastoise",  listOf("water"),             Stats(79, 83, 100, 85, 105, 78),  3),
            species(18,  "pidgeot",    listOf("normal", "flying"),  Stats(83, 80, 75, 70, 70, 101),   3),
            species(45,  "vileplume",  listOf("grass", "poison"),   Stats(75, 80, 85, 110, 90, 50),   3),
            species(59,  "arcanine",   listOf("fire"),              Stats(90, 110, 80, 100, 80, 95),  3),
            species(62,  "poliwrath",  listOf("water", "fighting"), Stats(90, 95, 95, 70, 90, 70),    3),
            species(65,  "alakazam",   listOf("psychic"),           Stats(55, 50, 45, 135, 95, 120),  3),
            species(68,  "machamp",    listOf("fighting"),          Stats(90, 130, 80, 65, 85, 55),   3),
            species(76,  "golem",      listOf("rock", "ground"),    Stats(80, 120, 130, 55, 65, 45),  3),
            species(94,  "gengar",     listOf("ghost", "poison"),   Stats(60, 65, 60, 130, 75, 110),  3),
            species(121, "starmie",    listOf("water", "psychic"),  Stats(60, 75, 85, 100, 85, 115),  3),
            species(143, "snorlax",    listOf("normal"),            Stats(160, 110, 65, 65, 110, 30), 3),

            species(144, "articuno",   listOf("ice", "flying"),     Stats(90, 85, 100, 95, 125, 85),    RogueSpecies.BOSS_TIER),
            species(145, "zapdos",     listOf("electric", "flying"),Stats(90, 90, 85, 125, 90, 100),    RogueSpecies.BOSS_TIER),
            species(146, "moltres",    listOf("fire", "flying"),    Stats(90, 100, 90, 125, 85, 90),    RogueSpecies.BOSS_TIER),
            species(149, "dragonite",  listOf("dragon", "flying"),  Stats(91, 134, 95, 100, 100, 80),   RogueSpecies.BOSS_TIER),
            species(150, "mewtwo",     listOf("psychic"),           Stats(106, 110, 90, 154, 90, 130),  RogueSpecies.BOSS_TIER)
        )
    }
}
