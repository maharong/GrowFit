package com.github.maharong.growfit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userStateRepository: UserStateRepository
) : ViewModel() {

    data class VibrationUiState(
        val isLoaded: Boolean = false,

        val vibrateEnabled: Boolean = true,
        val vibrateLastSecondsEnabled: Boolean = true,
        val vibrateOnStepChange: Boolean = true,
        val vibrateOnPresetComplete: Boolean = true,
        val vibrateLastSeconds: Int = 5
    )

    private val _uiState = MutableStateFlow(VibrationUiState())
    val uiState: StateFlow<VibrationUiState> = _uiState

    // 현재 로딩된 엔티티를 들고 있다가, 설정 바뀔 때마다 같이 저장
    private var entity: UserStateEntity? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val state = userStateRepository.load()
            entity = state

            _uiState.value = VibrationUiState(
                isLoaded = true,
                vibrateEnabled = state.vibrateEnabled,
                vibrateLastSecondsEnabled = state.vibrateLastSecondsEnabled,
                vibrateOnStepChange = state.vibrateOnStepChange,
                vibrateOnPresetComplete = state.vibrateOnPresetComplete,
                vibrateLastSeconds = state.vibrateLastSeconds
            )
        }
    }

    private fun save() {
        val currentEntity = entity ?: return
        val ui = _uiState.value

        currentEntity.vibrateEnabled = ui.vibrateEnabled
        currentEntity.vibrateLastSecondsEnabled = ui.vibrateLastSecondsEnabled
        currentEntity.vibrateOnStepChange = ui.vibrateOnStepChange
        currentEntity.vibrateOnPresetComplete = ui.vibrateOnPresetComplete
        currentEntity.vibrateLastSeconds = ui.vibrateLastSeconds.coerceAtLeast(1)

        viewModelScope.launch {
            userStateRepository.save(currentEntity)
        }
    }

    fun setVibrateEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrateEnabled = enabled) }
        save()
    }

    fun setLastSecondsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrateLastSecondsEnabled = enabled) }
        save()
    }

    fun setStepChangeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrateOnStepChange = enabled) }
        save()
    }

    fun setPresetCompleteEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrateOnPresetComplete = enabled) }
        save()
    }

    fun setLastSeconds(value: Int) {
        val safe = value.coerceIn(1, 60) // 1~60초 사이로 제한
        _uiState.update { it.copy(vibrateLastSeconds = safe) }
        save()
    }
}