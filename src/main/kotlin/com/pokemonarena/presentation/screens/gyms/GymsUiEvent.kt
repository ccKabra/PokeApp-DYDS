package com.pokemonarena.presentation.screens.gyms

import com.pokemonarena.domain.entity.Region

sealed interface GymsUiEvent {
    data class Load(val region: Region) : GymsUiEvent
}
