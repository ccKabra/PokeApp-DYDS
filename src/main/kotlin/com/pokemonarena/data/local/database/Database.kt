package com.pokemonarena.data.local.database

import com.pokemonarena.core.Constants
import com.pokemonarena.domain.entity.Economy
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object FavoriteCards : Table("favorite_cards") {
    val id          = integer("id").autoIncrement()
    val cardId      = varchar("card_id", 50).uniqueIndex()
    val pokemonName = varchar("pokemon_name", 100)
    val imageSmall  = varchar("image_url_small", 500)
    val rarity      = varchar("rarity", 100).nullable()
    val setName     = varchar("set_name", 200)
    
    val statHp      = integer("stat_hp").default(0)
    val statAtk     = integer("stat_attack").default(0)
    val statDef     = integer("stat_defense").default(0)
    val statSpAtk   = integer("stat_sp_attack").default(0)
    val statSpDef   = integer("stat_sp_defense").default(0)
    val statSpd     = integer("stat_speed").default(0)
    val primaryType = varchar("primary_type", 50).default("normal")
    
    val inTeam      = bool("in_team").default(false)
    val heldItem    = varchar("held_item", 50).nullable()
    val timesUsed   = integer("times_used").default(0)
    override val primaryKey = PrimaryKey(id)
}

object ItemInventory : Table("item_inventory") {
    val itemId   = varchar("item_id", 50).uniqueIndex()
    val quantity = integer("quantity").default(0)
}

object BattleHistory : Table("battle_history") {
    val id               = integer("id").autoIncrement()
    val gymName          = varchar("gym_name", 200)
    val playerCardName   = varchar("player_card_name", 200)
    val botCardName      = varchar("bot_card_name", 200)
    val winner           = varchar("winner", 10)
    val playerScore      = float("player_score")
    val botScore         = float("bot_score")
    val weatherCondition = varchar("weather_condition", 50)
    val date             = varchar("date", 50)
    val coinsDelta       = integer("coins_delta").default(0)
    override val primaryKey = PrimaryKey(id)
}

object GymBadges : Table("gym_badges") {
    val gymName    = varchar("gym_name", 200).uniqueIndex()
    val earnedDate = varchar("earned_date", 50)
}

object UserStatisticsTable : Table("user_statistics") {
    val id              = integer("id").default(1)
    val totalBattles    = integer("total_battles").default(0)
    val totalWins       = integer("total_wins").default(0)
    val totalLosses     = integer("total_losses").default(0)
    val currentStreak   = integer("current_streak").default(0)
    val bestStreak      = integer("best_streak").default(0)
    val favoritePokemon = varchar("favorite_pokemon", 100).nullable()
    val coins           = integer("coins").default(Economy.STARTING_COINS)
    override val primaryKey = PrimaryKey(id)
}

object UserProfiles : Table("user_profile") {
    val id     = integer("id").default(1)
    val name   = varchar("name", 50)
    val gender = varchar("gender", 10)
    override val primaryKey = PrimaryKey(id)
}

object DatabaseFactory {
    fun init() {
        val dbPath = File(System.getProperty("user.home"), ".pokemonarena/${Constants.DB_NAME}")
        dbPath.parentFile.mkdirs()
        Database.connect(url = "jdbc:sqlite:${dbPath.absolutePath}", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(FavoriteCards, BattleHistory, UserStatisticsTable, ItemInventory, GymBadges, UserProfiles)
        }
    }
}
