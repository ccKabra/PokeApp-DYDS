package com.pokemonarena.presentation.screens.profile

import com.pokemonarena.domain.entity.PlayerGender
import com.pokemonarena.domain.entity.PlayerProfile
import com.pokemonarena.domain.usecase.GetPlayerProfileUseCase
import com.pokemonarena.domain.usecase.SavePlayerProfileUseCase
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    object Missing : ProfileUiState
    data class Ready(val profile: PlayerProfile) : ProfileUiState
}

sealed interface ProfileUiEvent {
    data class Save(val name: String, val gender: PlayerGender) : ProfileUiEvent
}

class ProfileViewModel(
    getProfile: GetPlayerProfileUseCase,
    private val saveProfile: SavePlayerProfileUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            getProfile.execute().collect { profile ->
                _uiState.value = profile?.let { ProfileUiState.Ready(it) } ?: ProfileUiState.Missing
            }
        }
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            is ProfileUiEvent.Save -> scope.launch {
                runCatching { saveProfile.execute(PlayerProfile(event.name, event.gender)) }
            }
        }
    }
}
