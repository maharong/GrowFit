package com.github.maharong.growfit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 진동 설정을 관리하는 ViewModel.
 *
 * - UserStateEntity 로드 후 UI 상태 초기화
 * - 스위치 변경 시 즉시 DB 저장
 */
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

    /**
     * UserStateEntity를 로드하여 UI 상태를 구성한다.
     */
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

    /**
     * UI 상태 → DB 저장 (실시간 반영).
     */
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

    // 아래 setter 함수들은 모두:
    // 1) UI 상태 업데이트
    // 2) save() 호출
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