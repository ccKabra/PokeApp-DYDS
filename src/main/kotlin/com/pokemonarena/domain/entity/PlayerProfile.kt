package com.pokemonarena.domain.entity

enum class PlayerGender(val displayName: String) {
    MALE("Hombre"),
    FEMALE("Mujer")
}

data class PlayerProfile(val name: String, val gender: PlayerGender)
