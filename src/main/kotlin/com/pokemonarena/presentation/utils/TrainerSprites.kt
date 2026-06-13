package com.pokemonarena.presentation.utils

import com.pokemonarena.domain.entity.PlayerGender

val PlayerGender.spriteUrl: String
    get() = when (this) {
        PlayerGender.MALE   -> "https://play.pokemonshowdown.com/sprites/trainers/red.png"
        PlayerGender.FEMALE -> "https://play.pokemonshowdown.com/sprites/trainers/leaf.png"
    }
