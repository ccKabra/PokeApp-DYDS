package com.pokemonarena.presentation.screens.mine

import com.pokemonarena.domain.usecase.GetUserCoinsUseCase
import com.pokemonarena.domain.usecase.MineCoinsUseCase
import com.pokemonarena.domain.usecase.RegisterAimShotUseCase
import com.pokemonarena.presentation.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class MineViewModel(
    private val mineCoins: MineCoinsUseCase,
    getUserCoins: GetUserCoinsUseCase,
    private val aimShot: RegisterAimShotUseCase,
    private val random: Random = Random.Default
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MineUiState())
    val uiState: StateFlow<MineUiState> = _uiState.asStateFlow()

    private val digMutex = Mutex()
    private var deflateJob: Job? = null

    init {
        scope.launch { getUserCoins.execute().collect { c -> _uiState.update { it.copy(coins = c) } } }
    }

    fun onEvent(event: MineUiEvent) {
        when (event) {
            is MineUiEvent.Dig     -> scope.launch { dig() }
            is MineUiEvent.AimHit  -> scope.launch { registerAim(aimShot.registerHit(event.sizeFraction)) }
            is MineUiEvent.AimMiss -> scope.launch { registerAim(aimShot.registerMiss()) }
        }
    }

    private fun registerAim(delta: Int) {
        _uiState.update {
            it.copy(lastAimDelta = delta, aimShots = it.aimShots + 1, aimNet = it.aimNet + delta)
        }
    }

    private suspend fun dig() = digMutex.withLock {
        if (_uiState.value.isBroken) return@withLock

        val reward    = mineCoins.execute()
        val inflation = MIN_INFLATION + random.nextFloat() * (MAX_INFLATION - MIN_INFLATION)
        val pressure  = _uiState.value.pressure + inflation

        _uiState.update {
            it.copy(lastReward = reward,
                    totalMined = it.totalMined + reward.coins,
                    clicks     = it.clicks + 1,
                    pressure   = pressure.coerceAtMost(1f))
        }

        deflateJob?.cancel()
        if (pressure >= 1f) {
            explode()
        } else {
            deflateJob = scope.launch {
                delay(DEFLATE_AFTER_MS)
                _uiState.update { it.copy(pressure = 0f) }
            }
        }
    }

    private fun explode() {
        _uiState.update { it.copy(isBroken = true, cooldownSeconds = COOLDOWN_SECONDS) }
        scope.launch {
            repeat(COOLDOWN_SECONDS) {
                delay(1_000)
                _uiState.update { it.copy(cooldownSeconds = (it.cooldownSeconds - 1).coerceAtLeast(0)) }
            }
            _uiState.update { it.copy(isBroken = false, cooldownSeconds = 0, pressure = 0f) }
        }
    }

    companion object {
        const val MIN_INFLATION    = 0.07f
        const val MAX_INFLATION    = 0.30f
        const val DEFLATE_AFTER_MS = 1_500L
        const val COOLDOWN_SECONDS = 6
    }
}
