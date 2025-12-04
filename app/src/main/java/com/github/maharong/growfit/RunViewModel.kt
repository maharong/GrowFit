package com.github.maharong.growfit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunViewModel @Inject constructor(
    private val userStateManager: UserStateManager,
    private val presetRepository: PresetRepository
) : ViewModel() {

    data class RunUiState(
        val isLoading: Boolean = true,
        val presetName: String = "",
        val steps: List<PresetStepEntity> = emptyList(),
        val currentIndex: Int = 0,
        val remainingSeconds: Int? = null,
        val currentStep: PresetStepEntity? = null,
        val currentStepsTaken: Int = 0, // 걸음 수
        val isFinished: Boolean = false,
        val isPaused: Boolean = false   // 타이머 일시정지 여부
    )

    sealed class RunEvent {
        object PreAlert : RunEvent()           // n초 전 진동
        object RoutineFinished : RunEvent()    // 루틴 전체 완료
        data class Error(val message: String) : RunEvent()
    }

    private val _uiState = MutableStateFlow(RunUiState())
    val uiState: StateFlow<RunUiState> = _uiState

    private val _events = MutableSharedFlow<RunEvent>()
    val events: SharedFlow<RunEvent> = _events

    private var timerJob: Job? = null
    private val preAlertSeconds = 5  // n초 전 (추후 설정값으로 빼도 됨)

    fun start() {
        // 이미 시작했다면 다시 안함
        if (!_uiState.value.isLoading) return

        viewModelScope.launch {
            try {
                val selectedId = userStateManager.getSelectedPresetId()
                if (selectedId == null) {
                    _events.emit(RunEvent.Error("선택된 프리셋이 없습니다."))
                    return@launch
                }

                val presetWithSteps = presetRepository.getPresetWithSteps(selectedId)
                if (presetWithSteps == null || presetWithSteps.steps.isEmpty()) {
                    _events.emit(RunEvent.Error("프리셋에 스텝이 없습니다."))
                    return@launch
                }

                val sortedSteps = presetWithSteps.steps.sortedBy { it.order }

                _uiState.value = RunUiState(
                    isLoading = false,
                    presetName = presetWithSteps.preset.name,
                    steps = sortedSteps,
                    currentIndex = 0,
                    currentStep = sortedSteps[0],
                    remainingSeconds = sortedSteps[0].durationSec,
                    currentStepsTaken = 0,
                    isFinished = false,
                    isPaused = false
                )

                startTimerIfNeeded(sortedSteps[0])
            } catch (_: Exception) {
                _events.emit(RunEvent.Error("루틴을 시작할 수 없습니다."))
            }
        }
    }

    private fun startTimerIfNeeded(step: PresetStepEntity?) {
        timerJob?.cancel()
        val duration = step?.durationSec ?: 0

        // duration이 없거나 0이하이면 타이머 필요 없음
        if (duration <= 0) return

        timerJob = viewModelScope.launch {
            try {
                var remaining = duration
                while (remaining > 0) {
                    // 일시정지 상태면 잠깐씩 쉬면서 상태만 기다림
                    if (_uiState.value.isPaused) {
                        delay(200L)
                        continue
                    }

                    _uiState.update { it.copy(remainingSeconds = remaining) }

                    if (remaining == preAlertSeconds) {
                        _events.emit(RunEvent.PreAlert)
                    }

                    delay(1000L)
                    remaining--
                }

                // 타이머 끝 -> 자동 다음 스텝
                moveToNextStep()
            } catch (_: CancellationException) {
                // timer 취소되면 여기로 빠짐 -> 아무것도 안 함
            }
        }
    }

    fun moveToNextStep() {
        timerJob?.cancel()

        val state = _uiState.value
        val nextIndex = state.currentIndex + 1

        // 마지막 스텝까지 완료
        if (nextIndex >= state.steps.size) {
            viewModelScope.launch {
                // 보상 지급 + todayComplete 처리
                userStateManager.onPresetCompleted()
                _uiState.update {
                    it.copy(
                        isFinished = true,
                        currentStep = null,
                        remainingSeconds = null,
                        isPaused = false
                    )
                }
                _events.emit(RunEvent.RoutineFinished)
            }
            return
        }

        val nextStep = state.steps[nextIndex]
        _uiState.update {
            it.copy(
                currentIndex = nextIndex,
                currentStep = nextStep,
                remainingSeconds = nextStep.durationSec,
                currentStepsTaken = 0,
                isPaused = false
            )
        }

        startTimerIfNeeded(nextStep)
    }

    /**
     * COUNT / WALKING / RUNNING 같은 스텝에서
     * 사용자가 "다음 스텝" 버튼을 눌렀을 때 호출
     */
    fun onUserCompleteCurrentStep() {
        moveToNextStep()
    }

    /**
     * 걸음 수 업데이트 (센서 연동 후 Fragment에서 호출)
     */
    fun updateStepCount(currentSteps: Int) {
        _uiState.update { it.copy(currentStepsTaken = currentSteps) }
    }

    fun togglePause() {
        _uiState.update { current ->
            current.copy(isPaused = !current.isPaused)
        }
    }

    fun cancelRoutine() {
        timerJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}