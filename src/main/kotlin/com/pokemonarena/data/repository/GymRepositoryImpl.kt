package com.pokemonarena.data.repository

import com.pokemonarena.data.external.source_a.artworkUrl
import com.pokemonarena.domain.entity.*
import com.pokemonarena.domain.repository.GymRepository

class GymRepositoryImpl : GymRepository {

    override suspend fun getGyms(): List<Gym> =
        GYM_CONFIGS.map { cfg ->
            Gym(name = cfg.name, city = "${cfg.city}, ${cfg.country}",
                latitude = cfg.lat, longitude = cfg.lon, typeSpecialty = cfg.typeSpecialty,
                cardPool = cfg.cardPool.map { it.toCard(cfg.region) }, difficulty = cfg.difficulty,
                badgeImageUrl = badgeUrlFor(cfg),
                region = cfg.region)
        }

    override suspend fun getGymByName(name: String): Gym? =
        getGyms().firstOrNull { it.name == name }

    override suspend fun getLeagues(): List<League> =
        LEAGUE_CONFIGS.map { cfg ->
            League(region = cfg.region, difficulty = cfg.difficulty,
                   opponents = cfg.opponents.map { opp ->
                       LeagueOpponent(opp.name, opp.specialty,
                                      opp.pool.map { it.toCard(cfg.region) },
                                      imageUrl = "https://play.pokemonshowdown.com/sprites/trainers/${opp.sprite}.png")
                   })
        }

    private fun badgeUrlFor(cfg: GymConfig): String {
        val regionalIndex = GYM_CONFIGS.filter { it.region == cfg.region }.indexOfFirst { it.name == cfg.name }
        val badgeNumber   = regionalIndex + 1 + 8 * cfg.region.ordinal
        return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/badges/$badgeNumber.png"
    }

    data class GymConfig(
        val name: String, val city: String, val country: String,
        val lat: Double, val lon: Double, val typeSpecialty: String,
        val difficulty: Int, val region: Region,
        val cardPool: List<BotCardConfig>
    ) {
        init {
            require(cardPool.size >= 3) { "Pool de $name necesita al menos 3 cartas" }
            require(cardPool.all { it.itemId != null }) { "Todas las cartas del pool de $name deben tener item" }
        }
    }

    data class OpponentConfig(val name: String, val specialty: String, val sprite: String,
                              val pool: List<BotCardConfig>)

    data class LeagueConfig(val region: Region, val difficulty: Int, val opponents: List<OpponentConfig>)

    data class BotCardConfig(val pokeId: Int, val pokeName: String, val types: List<String>,
                             val stats: Stats, val itemId: String? = null)

    companion object {
        internal val GYM_CONFIGS: List<GymConfig> = listOf(
            GymConfig("Gimnasio Plateada", "El Cairo", "Egipto", 30.0444, 31.2357, "rock", 1, Region.KANTO, listOf(
                BotCardConfig(95, "onix",      listOf("rock","ground"), Stats(35,45,160,30,45,70),   "eviolite"),
                BotCardConfig(74, "geodude",   listOf("rock","ground"), Stats(40,80,100,30,30,20),   "assault-vest"),
                BotCardConfig(111,"rhyhorn",   listOf("ground","rock"), Stats(80,85,95,30,30,25),    "choice-band"),
                BotCardConfig(141,"kabutops",  listOf("rock","water"),  Stats(60,115,105,65,70,80),  "life-orb"),
                BotCardConfig(142,"aerodactyl",listOf("rock","flying"), Stats(80,105,65,60,75,130),  "choice-scarf"),
                BotCardConfig(76, "golem",     listOf("rock","ground"), Stats(80,120,130,55,65,45),  "choice-band"),
                BotCardConfig(139,"omastar",   listOf("rock","water"),  Stats(70,60,125,115,70,55),  "choice-specs"),
                BotCardConfig(75, "graveler",  listOf("rock","ground"), Stats(55,95,115,45,45,35),   "eviolite")
            )),
            GymConfig("Gimnasio Azulona", "Venecia", "Italia", 45.4408, 12.3155, "water", 2, Region.KANTO, listOf(
                BotCardConfig(121,"starmie",   listOf("water","psychic"), Stats(60,75,85,100,85,115), "choice-specs"),
                BotCardConfig(131,"lapras",    listOf("water","ice"),     Stats(130,85,80,85,95,60),  "assault-vest"),
                BotCardConfig(134,"vaporeon",  listOf("water"),           Stats(130,65,60,110,95,65), "life-orb"),
                BotCardConfig(73, "tentacruel",listOf("water","poison"),  Stats(80,70,65,80,120,100), "eviolite"),
                BotCardConfig(9,  "blastoise", listOf("water"),           Stats(79,83,100,85,105,78), "assault-vest"),
                BotCardConfig(130,"gyarados",  listOf("water","flying"),  Stats(95,125,79,60,100,81), "choice-band"),
                BotCardConfig(117,"seadra",    listOf("water"),           Stats(55,65,95,95,45,85),   "wide-lens"),
                BotCardConfig(62, "poliwrath", listOf("water","fighting"),Stats(90,95,95,70,90,70),   "eviolite")
            )),
            GymConfig("Gimnasio Lavapolis", "Honolulu", "Hawaii, EEUU", 21.3069, -157.8583, "fire", 2, Region.KANTO, listOf(
                BotCardConfig(59, "arcanine",  listOf("fire"),          Stats(90,110,80,100,80,95),  "choice-band"),
                BotCardConfig(136,"flareon",   listOf("fire"),          Stats(65,130,60,95,110,65),  "choice-band"),
                BotCardConfig(146,"moltres",   listOf("fire","flying"), Stats(90,100,90,125,85,90),  "life-orb"),
                BotCardConfig(78, "rapidash",  listOf("fire"),          Stats(65,100,70,80,80,105),  "choice-scarf"),
                BotCardConfig(38, "ninetales", listOf("fire"),          Stats(73,76,75,81,100,100),  "choice-specs"),
                BotCardConfig(126,"magmar",    listOf("fire"),          Stats(65,95,57,100,85,93),   "life-orb"),
                BotCardConfig(6,  "charizard", listOf("fire","flying"), Stats(78,84,78,109,85,100),  "choice-specs"),
                BotCardConfig(77, "ponyta",    listOf("fire"),          Stats(50,85,55,65,65,90),    "wide-lens")
            )),
            GymConfig("Gimnasio Lavanda", "Edimburgo", "Escocia", 55.9533, -3.1883, "ghost", 3, Region.KANTO, listOf(
                BotCardConfig(94, "gengar",    listOf("ghost","poison"), Stats(60,65,60,130,75,110), "choice-specs"),
                BotCardConfig(93, "haunter",   listOf("ghost","poison"), Stats(45,50,45,115,55,95),  "eviolite"),
                BotCardConfig(92, "gastly",    listOf("ghost","poison"), Stats(30,35,30,100,35,80),  "choice-specs"),
                BotCardConfig(42, "golbat",    listOf("poison","flying"),Stats(75,80,70,65,75,90),   "assault-vest"),
                BotCardConfig(24, "arbok",     listOf("poison"),         Stats(60,95,69,65,79,80),   "choice-band"),
                BotCardConfig(110,"weezing",   listOf("poison"),         Stats(65,90,120,85,70,60),  "eviolite"),
                BotCardConfig(89, "muk",       listOf("poison"),         Stats(105,105,75,65,100,50),"assault-vest"),
                BotCardConfig(105,"marowak",   listOf("ground"),         Stats(60,80,110,50,80,45),  "choice-band")
            )),
            GymConfig("Gimnasio Verdalia", "Manaos", "Brasil", -3.1190, -60.0217, "grass", 2, Region.KANTO, listOf(
                BotCardConfig(3,  "venusaur",  listOf("grass","poison"), Stats(80,82,83,100,100,80),  "eviolite"),
                BotCardConfig(103,"exeggutor", listOf("grass","psychic"),Stats(95,95,85,125,65,55),   "choice-specs"),
                BotCardConfig(114,"tangela",   listOf("grass"),          Stats(65,55,115,100,40,60),  "eviolite"),
                BotCardConfig(71, "victreebel",listOf("grass","poison"), Stats(80,105,65,100,70,70),  "life-orb"),
                BotCardConfig(45, "vileplume", listOf("grass","poison"), Stats(75,80,85,110,90,50),   "assault-vest"),
                BotCardConfig(47, "parasect",  listOf("bug","grass"),    Stats(60,95,80,60,80,30),    "choice-band"),
                BotCardConfig(2,  "ivysaur",   listOf("grass","poison"), Stats(60,62,63,80,80,60),    "eviolite"),
                BotCardConfig(44, "gloom",     listOf("grass","poison"), Stats(60,65,70,85,75,40),    "wide-lens")
            )),
            GymConfig("Gimnasio Voltio", "Nueva York", "EEUU", 40.7128, -74.0060, "electric", 3, Region.KANTO, listOf(
                BotCardConfig(135,"jolteon",   listOf("electric"),         Stats(65,65,60,110,95,130), "choice-scarf"),
                BotCardConfig(101,"electrode", listOf("electric"),         Stats(60,50,70,80,80,140),  "choice-scarf"),
                BotCardConfig(82, "magneton",  listOf("electric","steel"), Stats(50,60,95,120,70,70),  "assault-vest"),
                BotCardConfig(26, "raichu",    listOf("electric"),         Stats(60,90,55,90,80,110),  "life-orb"),
                BotCardConfig(125,"electabuzz",listOf("electric"),         Stats(65,83,57,95,85,105),  "choice-specs"),
                BotCardConfig(145,"zapdos",    listOf("electric","flying"),Stats(90,90,85,125,90,100), "life-orb"),
                BotCardConfig(25, "pikachu",   listOf("electric"),         Stats(35,55,40,50,50,90),   "eviolite"),
                BotCardConfig(100,"voltorb",   listOf("electric"),         Stats(40,30,50,55,55,100),  "wide-lens")
            )),
            GymConfig("Gimnasio Psíquico", "Katmandú", "Nepal", 27.7172, 85.3240, "psychic", 4, Region.KANTO, listOf(
                BotCardConfig(65, "alakazam",  listOf("psychic"),         Stats(55,50,45,135,95,120),  "choice-specs"),
                BotCardConfig(97, "hypno",     listOf("psychic"),         Stats(85,73,70,73,115,67),   "assault-vest"),
                BotCardConfig(122,"mr. mime",  listOf("psychic"),         Stats(40,45,65,100,120,90),  "eviolite"),
                BotCardConfig(124,"jynx",      listOf("ice","psychic"),   Stats(65,50,35,115,95,95),   "eviolite"),
                BotCardConfig(80, "slowbro",   listOf("psychic","water"), Stats(95,75,110,100,80,30),  "assault-vest"),
                BotCardConfig(64, "kadabra",   listOf("psychic"),         Stats(40,35,30,120,70,105),  "choice-scarf"),
                BotCardConfig(121,"starmie",   listOf("water","psychic"), Stats(60,75,85,100,85,115),  "life-orb"),
                BotCardConfig(103,"exeggutor", listOf("grass","psychic"), Stats(95,95,85,125,65,55),   "choice-specs")
            )),
            GymConfig("Gimnasio Glaciar", "Reikiavik", "Islandia", 64.1355, -21.8954, "ice", 4, Region.KANTO, listOf(
                BotCardConfig(144,"articuno",  listOf("ice","flying"),  Stats(90,85,100,95,125,85),  "assault-vest"),
                BotCardConfig(131,"lapras",    listOf("water","ice"),   Stats(130,85,80,85,95,60),   "life-orb"),
                BotCardConfig(87, "dewgong",   listOf("water","ice"),   Stats(90,70,80,70,95,70),    "eviolite"),
                BotCardConfig(91, "cloyster",  listOf("water","ice"),   Stats(50,95,180,85,45,70),   "eviolite"),
                BotCardConfig(124,"jynx",      listOf("ice","psychic"), Stats(65,50,35,115,95,95),   "choice-specs"),
                BotCardConfig(55, "golduck",   listOf("water"),         Stats(80,82,78,95,80,85),    "wide-lens"),
                BotCardConfig(117,"seadra",    listOf("water"),         Stats(55,65,95,95,45,85),    "choice-specs"),
                BotCardConfig(62, "poliwrath", listOf("water","fighting"),Stats(90,95,95,70,90,70),  "choice-band")
            )),

            GymConfig("Gimnasio Violeta", "Tokio", "Japón", 35.6762, 139.6503, "flying", 2, Region.JOHTO, listOf(
                BotCardConfig(164,"noctowl",   listOf("normal","flying"), Stats(100,50,50,86,96,70),  "choice-specs"),
                BotCardConfig(176,"togetic",   listOf("fairy","flying"),  Stats(55,40,85,80,105,40),  "eviolite"),
                BotCardConfig(178,"xatu",      listOf("psychic","flying"),Stats(65,75,70,95,70,95),   "choice-scarf"),
                BotCardConfig(198,"murkrow",   listOf("dark","flying"),   Stats(60,85,42,85,42,91),   "life-orb"),
                BotCardConfig(227,"skarmory",  listOf("steel","flying"),  Stats(65,80,140,40,70,70),  "assault-vest")
            )),
            GymConfig("Gimnasio Azalea", "Kioto", "Japón", 35.0116, 135.7681, "bug", 2, Region.JOHTO, listOf(
                BotCardConfig(212,"scizor",    listOf("bug","steel"),    Stats(70,130,100,55,80,65),  "choice-band"),
                BotCardConfig(214,"heracross", listOf("bug","fighting"), Stats(80,125,75,40,95,85),   "choice-scarf"),
                BotCardConfig(168,"ariados",   listOf("bug","poison"),   Stats(70,90,70,60,70,40),    "eviolite"),
                BotCardConfig(166,"ledian",    listOf("bug","flying"),   Stats(55,35,50,55,110,85),   "wide-lens"),
                BotCardConfig(205,"forretress",listOf("bug","steel"),    Stats(75,90,140,60,60,40),   "assault-vest")
            )),
            GymConfig("Gimnasio Trigal", "Osaka", "Japón", 34.6937, 135.5023, "normal", 3, Region.JOHTO, listOf(
                BotCardConfig(241,"miltank",   listOf("normal"),         Stats(95,80,105,40,70,100),  "eviolite"),
                BotCardConfig(162,"furret",    listOf("normal"),         Stats(85,76,64,45,55,90),    "choice-scarf"),
                BotCardConfig(210,"granbull",  listOf("fairy"),          Stats(90,120,75,60,60,45),   "choice-band"),
                BotCardConfig(217,"ursaring",  listOf("normal"),         Stats(90,130,75,75,75,55),   "life-orb"),
                BotCardConfig(234,"stantler",  listOf("normal"),         Stats(73,95,62,85,65,85),    "wide-lens")
            )),
            GymConfig("Gimnasio Iris", "Sapporo", "Japón", 43.0618, 141.3545, "ghost", 3, Region.JOHTO, listOf(
                BotCardConfig(200,"misdreavus",listOf("ghost"),          Stats(60,60,60,85,85,85),    "eviolite"),
                BotCardConfig(197,"umbreon",   listOf("dark"),           Stats(95,65,110,60,130,65),  "assault-vest"),
                BotCardConfig(229,"houndoom",  listOf("dark","fire"),    Stats(75,90,50,110,80,95),   "choice-specs"),
                BotCardConfig(215,"sneasel",   listOf("dark","ice"),     Stats(55,95,55,35,75,115),   "choice-scarf"),
                BotCardConfig(198,"murkrow",   listOf("dark","flying"),  Stats(60,85,42,85,42,91),    "life-orb")
            )),
            GymConfig("Gimnasio Orquídea", "Seúl", "Corea del Sur", 37.5665, 126.9780, "fighting", 3, Region.JOHTO, listOf(
                BotCardConfig(237,"hitmontop", listOf("fighting"),       Stats(50,95,95,35,110,70),   "assault-vest"),
                BotCardConfig(214,"heracross", listOf("bug","fighting"), Stats(80,125,75,40,95,85),   "choice-band"),
                BotCardConfig(217,"ursaring",  listOf("normal"),         Stats(90,130,75,75,75,55),   "choice-band"),
                BotCardConfig(210,"granbull",  listOf("fairy"),          Stats(90,120,75,60,60,45),   "eviolite"),
                BotCardConfig(232,"donphan",   listOf("ground"),         Stats(90,120,120,60,60,50),  "assault-vest")
            )),
            GymConfig("Gimnasio Acero", "Busan", "Corea del Sur", 35.1796, 129.0756, "steel", 4, Region.JOHTO, listOf(
                BotCardConfig(208,"steelix",   listOf("steel","ground"), Stats(75,85,200,55,65,30),   "assault-vest"),
                BotCardConfig(212,"scizor",    listOf("bug","steel"),    Stats(70,130,100,55,80,65),  "life-orb"),
                BotCardConfig(227,"skarmory",  listOf("steel","flying"), Stats(65,80,140,40,70,70),   "eviolite"),
                BotCardConfig(205,"forretress",listOf("bug","steel"),    Stats(75,90,140,60,60,40),   "eviolite"),
                BotCardConfig(219,"magcargo",  listOf("fire","rock"),    Stats(60,50,120,90,80,30),   "choice-specs")
            )),
            GymConfig("Gimnasio Escarcha", "Taipéi", "Taiwán", 25.0330, 121.5654, "ice", 4, Region.JOHTO, listOf(
                BotCardConfig(221,"piloswine", listOf("ice","ground"),   Stats(100,100,80,60,60,50),  "choice-band"),
                BotCardConfig(215,"sneasel",   listOf("dark","ice"),     Stats(55,95,55,35,75,115),   "choice-scarf"),
                BotCardConfig(245,"suicune",   listOf("water"),          Stats(100,75,115,90,115,85), "assault-vest"),
                BotCardConfig(225,"delibird",  listOf("ice","flying"),   Stats(45,55,45,65,45,75),    "wide-lens"),
                BotCardConfig(230,"kingdra",   listOf("water","dragon"), Stats(75,95,95,95,95,85),    "life-orb")
            )),
            GymConfig("Gimnasio Dragón", "Hong Kong", "China", 22.3193, 114.1694, "dragon", 4, Region.JOHTO, listOf(
                BotCardConfig(230,"kingdra",   listOf("water","dragon"), Stats(75,95,95,95,95,85),    "choice-specs"),
                BotCardConfig(181,"ampharos",  listOf("electric"),       Stats(90,75,85,115,90,55),   "assault-vest"),
                BotCardConfig(248,"tyranitar", listOf("rock","dark"),    Stats(100,134,110,95,100,61),"choice-band"),
                BotCardConfig(169,"crobat",    listOf("poison","flying"),Stats(85,90,80,70,80,130),   "choice-scarf"),
                BotCardConfig(196,"espeon",    listOf("psychic"),        Stats(65,65,60,130,110,110), "life-orb")
            )),

            GymConfig("Gimnasio Férrico", "Sídney", "Australia", -33.8688, 151.2093, "rock", 3, Region.HOENN, listOf(
                BotCardConfig(306,"aggron",    listOf("steel","rock"),   Stats(70,110,180,60,60,50),  "assault-vest"),
                BotCardConfig(348,"armaldo",   listOf("rock","bug"),     Stats(75,125,100,70,80,45),  "choice-band"),
                BotCardConfig(346,"cradily",   listOf("rock","grass"),   Stats(86,81,97,81,107,43),   "eviolite"),
                BotCardConfig(369,"relicanth", listOf("water","rock"),   Stats(100,90,130,45,65,55),  "eviolite"),
                BotCardConfig(299,"nosepass",  listOf("rock"),           Stats(30,45,135,45,90,30),   "wide-lens")
            )),
            GymConfig("Gimnasio Puño", "Auckland", "Nueva Zelanda", -36.8485, 174.7633, "fighting", 3, Region.HOENN, listOf(
                BotCardConfig(297,"hariyama",  listOf("fighting"),       Stats(144,120,60,40,60,50),  "assault-vest"),
                BotCardConfig(286,"breloom",   listOf("grass","fighting"),Stats(60,130,80,60,60,70),  "choice-band"),
                BotCardConfig(308,"medicham",  listOf("fighting","psychic"),Stats(60,60,75,60,75,80), "choice-scarf"),
                BotCardConfig(257,"blaziken",  listOf("fire","fighting"),Stats(80,120,70,110,70,80),  "life-orb"),
                BotCardConfig(335,"zangoose",  listOf("normal"),         Stats(73,115,60,60,60,90),   "choice-band")
            )),
            GymConfig("Gimnasio Dínamo", "Ciudad de México", "México", 19.4326, -99.1332, "electric", 4, Region.HOENN, listOf(
                BotCardConfig(310,"manectric", listOf("electric"),       Stats(70,75,60,105,60,105),  "choice-specs"),
                BotCardConfig(311,"plusle",    listOf("electric"),       Stats(60,50,40,85,75,95),    "life-orb"),
                BotCardConfig(312,"minun",     listOf("electric"),       Stats(60,40,50,75,85,95),    "wide-lens"),
                BotCardConfig(309,"electrike", listOf("electric"),       Stats(40,45,40,65,40,65),    "eviolite")
            )),
            GymConfig("Gimnasio Caldera", "Lima", "Perú", -12.0464, -77.0428, "fire", 4, Region.HOENN, listOf(
                BotCardConfig(324,"torkoal",   listOf("fire"),           Stats(70,85,140,85,70,20),   "assault-vest"),
                BotCardConfig(323,"camerupt",  listOf("fire","ground"),  Stats(70,100,70,105,75,40),  "choice-specs"),
                BotCardConfig(257,"blaziken",  listOf("fire","fighting"),Stats(80,120,70,110,70,80),  "choice-band"),
                BotCardConfig(256,"combusken", listOf("fire","fighting"),Stats(60,85,60,85,60,55),    "eviolite")
            )),
            GymConfig("Gimnasio Equilibrio", "Bogotá", "Colombia", 4.7110, -74.0721, "normal", 4, Region.HOENN, listOf(
                BotCardConfig(289,"slaking",   listOf("normal"),         Stats(150,160,100,95,65,100),"choice-band"),
                BotCardConfig(264,"linoone",   listOf("normal"),         Stats(78,70,61,50,61,100),   "choice-scarf"),
                BotCardConfig(295,"exploud",   listOf("normal"),         Stats(104,91,63,91,73,68),   "choice-specs"),
                BotCardConfig(335,"zangoose",  listOf("normal"),         Stats(73,115,60,60,60,90),   "life-orb"),
                BotCardConfig(352,"kecleon",   listOf("normal"),         Stats(60,90,70,60,120,40),   "eviolite")
            )),
            GymConfig("Gimnasio Pluma", "Madrid", "España", 40.4168, -3.7038, "flying", 4, Region.HOENN, listOf(
                BotCardConfig(334,"altaria",   listOf("dragon","flying"),Stats(75,70,90,70,105,80),   "assault-vest"),
                BotCardConfig(279,"pelipper",  listOf("water","flying"), Stats(60,50,100,95,70,65),   "choice-specs"),
                BotCardConfig(277,"swellow",   listOf("normal","flying"),Stats(60,85,60,75,50,125),   "choice-scarf"),
                BotCardConfig(357,"tropius",   listOf("grass","flying"), Stats(99,68,83,72,87,51),    "eviolite")
            )),
            GymConfig("Gimnasio Mental", "Barcelona", "España", 41.3874, 2.1686, "psychic", 5, Region.HOENN, listOf(
                BotCardConfig(282,"gardevoir", listOf("psychic","fairy"),Stats(68,65,65,125,115,80),  "choice-specs"),
                BotCardConfig(338,"solrock",   listOf("rock","psychic"), Stats(90,95,85,55,65,70),    "choice-band"),
                BotCardConfig(337,"lunatone",  listOf("rock","psychic"), Stats(90,55,65,95,85,70),    "life-orb"),
                BotCardConfig(344,"claydol",   listOf("ground","psychic"),Stats(60,70,105,70,120,75), "assault-vest"),
                BotCardConfig(376,"metagross", listOf("steel","psychic"),Stats(80,135,130,95,90,70),  "choice-band")
            )),
            GymConfig("Gimnasio Marea", "Roma", "Italia", 41.9028, 12.4964, "water", 5, Region.HOENN, listOf(
                BotCardConfig(350,"milotic",   listOf("water"),          Stats(95,60,79,100,125,81),  "assault-vest"),
                BotCardConfig(272,"ludicolo",  listOf("water","grass"),  Stats(80,70,70,90,100,70),   "life-orb"),
                BotCardConfig(319,"sharpedo",  listOf("water","dark"),   Stats(70,120,40,95,40,95),   "choice-scarf"),
                BotCardConfig(321,"wailord",   listOf("water"),          Stats(170,90,45,90,45,60),   "choice-specs"),
                BotCardConfig(260,"swampert",  listOf("water","ground"), Stats(100,110,90,85,90,60),  "choice-band")
            ))
        )

        internal val LEAGUE_CONFIGS: List<LeagueConfig> = listOf(
            LeagueConfig(Region.KANTO, 5, listOf(
                OpponentConfig("Lorelei", "ice", "lorelei-gen3", listOf(
                    BotCardConfig(87, "dewgong", listOf("water","ice"),  Stats(90,70,80,70,95,70),    "eviolite"),
                    BotCardConfig(91, "cloyster",listOf("water","ice"),  Stats(50,95,180,85,45,70),   "assault-vest"),
                    BotCardConfig(131,"lapras",  listOf("water","ice"),  Stats(130,85,80,85,95,60),   "life-orb"),
                    BotCardConfig(124,"jynx",    listOf("ice","psychic"),Stats(65,50,35,115,95,95),   "choice-specs")
                )),
                OpponentConfig("Bruno", "fighting", "bruno-gen3", listOf(
                    BotCardConfig(68, "machamp",  listOf("fighting"),     Stats(90,130,80,65,85,55),  "choice-band"),
                    BotCardConfig(106,"hitmonlee",listOf("fighting"),     Stats(50,120,53,35,110,87), "choice-scarf"),
                    BotCardConfig(107,"hitmonchan",listOf("fighting"),    Stats(50,105,79,35,110,76), "assault-vest"),
                    BotCardConfig(95, "onix",     listOf("rock","ground"),Stats(35,45,160,30,45,70),  "eviolite")
                )),
                OpponentConfig("Agatha", "ghost", "agatha-gen3", listOf(
                    BotCardConfig(94, "gengar",  listOf("ghost","poison"), Stats(60,65,60,130,75,110),"choice-specs"),
                    BotCardConfig(93, "haunter", listOf("ghost","poison"), Stats(45,50,45,115,55,95), "eviolite"),
                    BotCardConfig(24, "arbok",   listOf("poison"),         Stats(60,95,69,65,79,80),  "choice-band"),
                    BotCardConfig(110,"weezing", listOf("poison"),         Stats(65,90,120,85,70,60), "assault-vest")
                )),
                OpponentConfig("Lance", "dragon", "lance", listOf(
                    BotCardConfig(149,"dragonite", listOf("dragon","flying"),Stats(91,134,95,100,100,80),"choice-band"),
                    BotCardConfig(130,"gyarados",  listOf("water","flying"), Stats(95,125,79,60,100,81), "life-orb"),
                    BotCardConfig(142,"aerodactyl",listOf("rock","flying"),  Stats(80,105,65,60,75,130), "choice-scarf"),
                    BotCardConfig(148,"dragonair", listOf("dragon"),         Stats(61,84,65,70,70,70),   "eviolite")
                ))
            )),
            LeagueConfig(Region.JOHTO, 5, listOf(
                OpponentConfig("Mento", "psychic", "will-gen2", listOf(
                    BotCardConfig(178,"xatu",     listOf("psychic","flying"),Stats(65,75,70,95,70,95),  "choice-scarf"),
                    BotCardConfig(196,"espeon",   listOf("psychic"),         Stats(65,65,60,130,110,110),"choice-specs"),
                    BotCardConfig(199,"slowking", listOf("psychic","water"), Stats(95,75,80,100,110,30), "assault-vest"),
                    BotCardConfig(203,"girafarig",listOf("normal","psychic"),Stats(70,80,65,90,65,85),   "life-orb")
                )),
                OpponentConfig("Koga", "poison", "koga-gen2", listOf(
                    BotCardConfig(169,"crobat",    listOf("poison","flying"),Stats(85,90,80,70,80,130), "choice-scarf"),
                    BotCardConfig(168,"ariados",   listOf("bug","poison"),   Stats(70,90,70,60,70,40),  "eviolite"),
                    BotCardConfig(205,"forretress",listOf("bug","steel"),    Stats(75,90,140,60,60,40), "assault-vest"),
                    BotCardConfig(211,"qwilfish",  listOf("water","poison"), Stats(65,95,75,55,55,85),  "choice-band")
                )),
                OpponentConfig("Karen", "dark", "karen-gen2", listOf(
                    BotCardConfig(197,"umbreon",  listOf("dark"),          Stats(95,65,110,60,130,65), "assault-vest"),
                    BotCardConfig(229,"houndoom", listOf("dark","fire"),   Stats(75,90,50,110,80,95),  "choice-specs"),
                    BotCardConfig(198,"murkrow",  listOf("dark","flying"), Stats(60,85,42,85,42,91),   "life-orb"),
                    BotCardConfig(215,"sneasel",  listOf("dark","ice"),    Stats(55,95,55,35,75,115),  "choice-scarf")
                )),
                OpponentConfig("Lance", "dragon", "lance-gen2", listOf(
                    BotCardConfig(250,"ho-oh",    listOf("fire","flying"),   Stats(106,130,90,110,154,90), "choice-band"),
                    BotCardConfig(249,"lugia",    listOf("psychic","flying"),Stats(106,90,130,90,154,110), "assault-vest"),
                    BotCardConfig(248,"tyranitar",listOf("rock","dark"),     Stats(100,134,110,95,100,61), "life-orb"),
                    BotCardConfig(230,"kingdra",  listOf("water","dragon"),  Stats(75,95,95,95,95,85),     "choice-specs")
                ))
            )),
            LeagueConfig(Region.HOENN, 5, listOf(
                OpponentConfig("Sixto", "dark", "sidney-gen3", listOf(
                    BotCardConfig(262,"mightyena",listOf("dark"),          Stats(70,90,70,60,60,70),   "choice-band"),
                    BotCardConfig(275,"shiftry",  listOf("grass","dark"),  Stats(90,100,60,90,60,80),  "life-orb"),
                    BotCardConfig(359,"absol",    listOf("dark"),          Stats(65,130,60,75,60,75),  "choice-scarf"),
                    BotCardConfig(332,"cacturne", listOf("grass","dark"),  Stats(70,115,60,115,60,55), "choice-specs")
                )),
                OpponentConfig("Fátima", "ghost", "phoebe-gen3", listOf(
                    BotCardConfig(356,"dusclops", listOf("ghost"),          Stats(40,70,130,60,130,25), "eviolite"),
                    BotCardConfig(354,"banette",  listOf("ghost"),          Stats(64,115,65,83,63,65),  "choice-band"),
                    BotCardConfig(302,"sableye",  listOf("dark","ghost"),   Stats(50,75,75,65,65,50),   "assault-vest"),
                    BotCardConfig(344,"claydol",  listOf("ground","psychic"),Stats(60,70,105,70,120,75),"assault-vest")
                )),
                OpponentConfig("Nívea", "ice", "glacia-gen3", listOf(
                    BotCardConfig(362,"glalie",  listOf("ice"),          Stats(80,80,80,80,80,80),     "wide-lens"),
                    BotCardConfig(365,"walrein", listOf("ice","water"),  Stats(110,80,90,95,90,65),    "assault-vest"),
                    BotCardConfig(364,"sealeo",  listOf("ice","water"),  Stats(90,60,70,75,70,45),     "eviolite"),
                    BotCardConfig(378,"regice",  listOf("ice"),          Stats(80,50,100,100,200,50),  "choice-specs")
                )),
                OpponentConfig("Dracón", "dragon", "drake-gen3", listOf(
                    BotCardConfig(373,"salamence",listOf("dragon","flying"),Stats(95,135,80,110,80,100),"choice-band"),
                    BotCardConfig(330,"flygon",   listOf("ground","dragon"),Stats(80,100,80,80,80,100), "choice-scarf"),
                    BotCardConfig(334,"altaria",  listOf("dragon","flying"),Stats(75,70,90,70,105,80),  "assault-vest"),
                    BotCardConfig(381,"latios",   listOf("dragon","psychic"),Stats(80,90,80,130,110,110),"choice-specs")
                ))
            ))
        )

        private fun BotCardConfig.toCard(region: Region): Card {
            val url     = artworkUrl(pokeId)
            val boosted = stats.scaledBy(RegionDifficulty.botStatMultiplier(region))
            val detail  = PokemonDetail(pokeId, pokeName, url, types, 0, 0, boosted, listOf(pokeName))
            return Card("bot-$pokeId", pokeName.replaceFirstChar { it.uppercase() }, url, url, "Rare", "Gym", detail,
                        heldItem = itemId?.let { ItemCatalog.byId(it) })
        }
    }
}
