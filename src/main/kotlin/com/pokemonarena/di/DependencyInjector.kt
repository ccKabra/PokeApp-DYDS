package com.pokemonarena.di

import com.pokemonarena.data.external.ItemsExternalSource
import com.pokemonarena.data.external.WeatherSource
import com.pokemonarena.data.external.broker.CardsSourceWithFallback
import com.pokemonarena.data.external.broker.PokemonCardsExternalSource
import com.pokemonarena.data.external.broker.PokemonDetailBroker
import com.pokemonarena.data.external.broker.PokemonDetailExternalSource
import com.pokemonarena.data.external.source_a.PokeApiExternalSource
import com.pokemonarena.data.external.source_b.TcgExternalSource
import com.pokemonarena.data.external.source_c.TcgDexExternalSource
import com.pokemonarena.data.local.dao.BattleHistoryDao
import com.pokemonarena.data.local.dao.FavoriteCardDao
import com.pokemonarena.data.local.dao.GymBadgeDao
import com.pokemonarena.data.local.dao.ItemInventoryDao
import com.pokemonarena.data.local.dao.RogueLivesDao
import com.pokemonarena.data.local.dao.RogueUpgradeDao
import com.pokemonarena.data.local.dao.UserProfileDao
import com.pokemonarena.data.local.dao.UserStatisticsDao
import com.pokemonarena.data.repository.*
import com.pokemonarena.domain.usecase.*
import com.pokemonarena.presentation.screens.battle.BattleViewModel
import com.pokemonarena.presentation.screens.collection.CollectionViewModel
import com.pokemonarena.presentation.screens.detail.CardDetailViewModel
import com.pokemonarena.presentation.screens.gyms.GymsViewModel
import com.pokemonarena.presentation.screens.home.HomeViewModel
import com.pokemonarena.presentation.navigation.NavigationViewModel
import com.pokemonarena.presentation.screens.items.ItemsViewModel
import com.pokemonarena.presentation.screens.league.LeagueViewModel
import com.pokemonarena.presentation.screens.mine.MineViewModel
import com.pokemonarena.presentation.screens.myteam.MyTeamViewModel
import com.pokemonarena.presentation.screens.profile.ProfileViewModel
import com.pokemonarena.presentation.screens.rogue.RogueViewModel
import com.pokemonarena.presentation.screens.statistics.StatisticsViewModel
import com.pokemonarena.core.Constants
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object DependencyInjector {

    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            install(Logging) { level = LogLevel.INFO }
            install(HttpTimeout) {
                connectTimeoutMillis = Constants.CONNECT_TIMEOUT_MS
                requestTimeoutMillis = Constants.REQUEST_TIMEOUT_MS
                socketTimeoutMillis  = Constants.READ_TIMEOUT_MS
            }
        }
    }

    private val pokeApiSource: PokeApiExternalSource by lazy { PokeApiExternalSource(httpClient) }
    private val tcgSource:     TcgExternalSource     by lazy { TcgExternalSource(httpClient) }
    private val tcgDexSource:  TcgDexExternalSource  by lazy { TcgDexExternalSource(httpClient) }
    private val weatherSource: WeatherSource         by lazy { WeatherSource(httpClient) }

    private val cardsSource: PokemonCardsExternalSource by lazy {
        CardsSourceWithFallback(primary = tcgDexSource, secondary = tcgSource)
    }

    private val pokemonDetailBroker: PokemonDetailExternalSource by lazy {
        PokemonDetailBroker(pokeApiSource, tcgSource)
    }

    private val favoriteCardDao:   FavoriteCardDao   by lazy { FavoriteCardDao() }
    private val battleHistoryDao:  BattleHistoryDao  by lazy { BattleHistoryDao() }
    private val userStatisticsDao: UserStatisticsDao by lazy { UserStatisticsDao() }
    private val itemInventoryDao:  ItemInventoryDao  by lazy { ItemInventoryDao() }
    private val gymBadgeDao:       GymBadgeDao       by lazy { GymBadgeDao() }
    private val userProfileDao:    UserProfileDao    by lazy { UserProfileDao() }
    private val rogueUpgradeDao:   RogueUpgradeDao   by lazy { RogueUpgradeDao() }
    private val rogueLivesDao:     RogueLivesDao     by lazy { RogueLivesDao() }

    private val itemsSource: ItemsExternalSource by lazy { ItemsExternalSource(httpClient) }

    private val pokemonRepository by lazy { PokemonRepositoryImpl(pokeApiSource, pokeApiSource) }
    private val cardRepository    by lazy { CardRepositoryImpl(pokemonDetailBroker, pokeApiSource, cardsSource, favoriteCardDao) }
    private val gymRepository     by lazy { GymRepositoryImpl() }
    private val weatherRepository by lazy { WeatherRepositoryImpl(weatherSource) }
    private val battleRepository  by lazy { BattleRepositoryImpl(battleHistoryDao, userStatisticsDao) }
    private val itemRepository    by lazy { ItemRepositoryImpl(itemsSource, itemInventoryDao) }
    private val badgeRepository   by lazy { BadgeRepositoryImpl(gymBadgeDao) }
    private val profileRepository by lazy { ProfileRepositoryImpl(userProfileDao) }
    private val roguePoolRepository by lazy { RoguePoolRepositoryImpl() }
    private val rogueMetaRepository by lazy { RogueMetaRepositoryImpl(rogueUpgradeDao, rogueLivesDao) }

    private val getPokemonsUseCase:      GetPokemonsUseCase      = GetPokemonsUseCaseImpl(pokemonRepository)
    private val getPokemonDetailUseCase: GetPokemonDetailUseCase = GetPokemonDetailUseCaseImpl(pokemonRepository)

    fun homeViewModel() = HomeViewModel(
        GetUserStatisticsUseCase(battleRepository),
        GetTeamUseCase(cardRepository),
        GetBattleHistoryUseCase(battleRepository),
        GetGymsUseCase(gymRepository),
        GetLeaguesUseCase(gymRepository),
        GetEarnedBadgesUseCase(badgeRepository)
    )

    fun collectionViewModel() = CollectionViewModel(
        getPokemonsUseCase,
        GetRegionProgressUseCase(gymRepository, badgeRepository)
    )

    fun cardDetailViewModel() = CardDetailViewModel(
        getPokemonDetailUseCase,
        GetCardsForPokemonUseCase(cardRepository),
        PurchaseCardUseCase(cardRepository, battleRepository),
        GetOwnedCardsUseCase(cardRepository),
        GetUserStatisticsUseCase(battleRepository)
    )

    fun gymsViewModel() = GymsViewModel(
        GetGymsUseCase(gymRepository),
        GetWeatherConditionUseCase(weatherRepository),
        GetTeamUseCase(cardRepository),
        GetRegionProgressUseCase(gymRepository, badgeRepository)
    )

    fun leagueViewModel() = LeagueViewModel(
        GetLeaguesUseCase(gymRepository),
        GetGymsUseCase(gymRepository),
        GetRegionProgressUseCase(gymRepository, badgeRepository),
        GetOwnedCardsUseCase(cardRepository),
        GetTeamUseCase(cardRepository),
        UpdateTeamUseCase(cardRepository),
        GetItemCatalogUseCase(itemRepository),
        GetItemInventoryUseCase(itemRepository),
        EquipItemUseCase(cardRepository, itemRepository),
        UnequipItemUseCase(cardRepository, itemRepository),
        CureFatigueUseCase(cardRepository, itemRepository),
        SimulateBattleUseCase(),
        SaveBattleResultUseCase(battleRepository, cardRepository,
                                UpdateStatisticsAfterBattleUseCase(battleRepository)),
        DropHeldItemUseCase(cardRepository),
        AwardBadgeIfFirstWinUseCase(badgeRepository, battleRepository, itemRepository)
    )

    fun myTeamViewModel() = MyTeamViewModel(
        GetOwnedCardsUseCase(cardRepository),
        GetTeamUseCase(cardRepository),
        UpdateTeamUseCase(cardRepository),
        GetUserStatisticsUseCase(battleRepository),
        SellCardUseCase(cardRepository, battleRepository, itemRepository),
        GetItemCatalogUseCase(itemRepository),
        GetItemInventoryUseCase(itemRepository),
        EquipItemUseCase(cardRepository, itemRepository),
        UnequipItemUseCase(cardRepository, itemRepository),
        CureFatigueUseCase(cardRepository, itemRepository)
    )

    fun itemsViewModel() = ItemsViewModel(
        GetItemCatalogUseCase(itemRepository),
        GetItemInventoryUseCase(itemRepository),
        GetUserCoinsUseCase(battleRepository),
        PurchaseItemUseCase(itemRepository, battleRepository)
    )

    fun battleViewModel() = BattleViewModel(
        GetGymByNameUseCase(gymRepository),
        GetTeamUseCase(cardRepository),
        GetWeatherConditionUseCase(weatherRepository),
        SimulateBattleUseCase(),
        SaveBattleResultUseCase(battleRepository, cardRepository,
                                UpdateStatisticsAfterBattleUseCase(battleRepository)),
        DropHeldItemUseCase(cardRepository),
        AwardBadgeIfFirstWinUseCase(badgeRepository, battleRepository, itemRepository)
    )

    fun statisticsViewModel() = StatisticsViewModel(
        GetUserStatisticsUseCase(battleRepository),
        GetBattleHistoryUseCase(battleRepository)
    )

    fun navigationViewModel() = NavigationViewModel(
        GetUserCoinsUseCase(battleRepository),
        GetEarnedBadgesUseCase(badgeRepository),
        GetRegionProgressUseCase(gymRepository, badgeRepository)
    )

    fun mineViewModel() = MineViewModel(
        MineCoinsUseCase(battleRepository),
        GetUserCoinsUseCase(battleRepository),
        RegisterAimShotUseCase(battleRepository)
    )

    fun profileViewModel() = ProfileViewModel(
        GetPlayerProfileUseCase(profileRepository),
        SavePlayerProfileUseCase(profileRepository)
    )

    fun rogueViewModel() = RogueViewModel(
        GetRoguePoolUseCase(roguePoolRepository),
        CashOutRogueRunUseCase(battleRepository),
        GetRogueMetaUseCase(rogueMetaRepository),
        PurchaseRogueUpgradeUseCase(rogueMetaRepository, battleRepository),
        GetUserCoinsUseCase(battleRepository),
        GetRogueLivesUseCase(rogueMetaRepository),
        ConsumeRogueLifeUseCase(rogueMetaRepository)
    )
}
