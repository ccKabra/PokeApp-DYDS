package com.pokemonarena.core

object Constants {
    
    const val POKEAPI_BASE_URL    = "https://pokeapi.co/api/v2"
    const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1"
    const val TCG_BASE_URL        = "https://api.pokemontcg.io/v2"
    const val TCGDEX_GRAPHQL_URL  = "https://api.tcgdex.net/v2/graphql"

    const val DEFAULT_LIMIT  = 20
    const val DEFAULT_OFFSET = 0
    const val TCG_PAGE_SIZE  = 250

    const val WINDOW_TITLE  = "PokeApp"
    const val WINDOW_WIDTH  = 1280
    const val WINDOW_HEIGHT = 800
    const val MIN_WIDTH     = 900
    const val MIN_HEIGHT    = 600

    const val DB_NAME = "pokemonarena.db"

    const val CONNECT_TIMEOUT_MS = 30_000L
    const val READ_TIMEOUT_MS    = 60_000L   
    const val REQUEST_TIMEOUT_MS = 90_000L
}
