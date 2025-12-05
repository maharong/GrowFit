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
        val isPaused: Boolean = false,   // 타이머 일시정지 여부
        // 완료 화면용
        val rewardMessage: String = "",
        val streakMessage: String = "",
        val commentMessage: String = "",
        val alreadyRewarded: Boolean = false,
        // 설정 값
        val vibrateEnabled: Boolean = true,
        val vibrateLastSecondsEnabled: Boolean = true,
        val vibrateOnStepChange: Boolean = true,
        val vibrateOnPresetComplete: Boolean = true,
        val vibrateLastSeconds: Int = 5
    )

    sealed class RunEvent {
        object PreAlert : RunEvent()           // n초 전 진동
        object RoutineFinished : RunEvent()    // 프리셋 전체 완료
        object StepChanged : RunEvent() // 스텝 변경
        data class Error(val message: String) : RunEvent()
    }

    private val _uiState = MutableStateFlow(RunUiState())
    val uiState: StateFlow<RunUiState> = _uiState

    private val _events = MutableSharedFlow<RunEvent>()
    val events: SharedFlow<RunEvent> = _events

    private var timerJob: Job? = null

    fun start() {
        // 이미 시작했다면 다시 안함
        if (!_uiState.value.isLoading) return

        viewModelScope.launch {
            try {
                val userState = userStateManager.getCurrentState()

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
                    isPaused = false,
                    vibrateEnabled = userState.vibrateEnabled,
                    vibrateLastSecondsEnabled = userState.vibrateLastSecondsEnabled,
                    vibrateOnStepChange = userState.vibrateOnStepChange,
                    vibrateOnPresetComplete = userState.vibrateOnPresetComplete,
                    vibrateLastSeconds = userState.vibrateLastSeconds
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

                    val current = _uiState.value
                    _uiState.update { it.copy(remainingSeconds = remaining) }


                    if (current.vibrateEnabled && current.vibrateLastSecondsEnabled) {
                        // 마지막 n초 전 진동 설정값이 스텝 설정보다 높은 경우 스텝에 맞춰 조절
                        val preAlertSeconds = current.vibrateLastSeconds
                            .coerceAtLeast(1)
                            .coerceAtMost(duration)

                        if (remaining == preAlertSeconds) {
                            _events.emit(RunEvent.PreAlert)
                        }
                    }

                    delay(1000L)
                    remaining--
                }

                // 타이머 끝 -> 자동 다음 스텝
                moveToNextStep()
            } catch (_: CancellationException) {
                // no-op
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
                // 보상 지급 + 오늘 운동 완료 처리
                val result = userStateManager.onPresetCompleted()
                val rewardMessage = if (result.alreadyReceived) {
                    "오늘 보상은 이미 받았어요."
                } else {
                    "EXP +${result.rewardExp} · 포인트 +${result.rewardPoints}"
                }

                val streakMessage = makeStreakMessage(result.streakDays)
                val commentMessage = makeCommentMessage(result.streakDays, result.alreadyReceived)

                _uiState.update {
                    it.copy(
                        isFinished = true,
                        currentStep = null,
                        remainingSeconds = null,
                        isPaused = false,
                        rewardMessage = rewardMessage,
                        streakMessage = streakMessage,
                        commentMessage = commentMessage,
                        alreadyRewarded = result.alreadyReceived
                    )
                }
                // 프리셋 완료 시 진동
                val ui = _uiState.value
                if (ui.vibrateEnabled && ui.vibrateOnPresetComplete) {
                    _events.emit(RunEvent.RoutineFinished)
                }
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
        // 스텝 변경 진동
        val ui = _uiState.value
        if (ui.vibrateEnabled && ui.vibrateOnStepChange) {
            viewModelScope.launch {
                _events.emit(RunEvent.StepChanged)
            }
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

    private fun makeStreakMessage(streak: Int): String {
        if (streak <= 0) return "오늘이 첫 운동이에요."

        return when (streak) {
            1 -> "오늘부터 1일차! 좋은 시작이에요."
            in 2..4 -> "연속 ${streak}일째 운동 중이에요. 계속 가볼까요?"
            in 5..9 -> "연속 ${streak}일째! 습관이 자리잡고 있어요."
            else -> "연속 ${streak}일째! 정말 대단해요! 앞으로도 계속 이어나가요!"
        }
    }

    private fun makeCommentMessage(streak: Int, already: Boolean): String {
        if (already) {
            return "오늘은 이미 멋지게 해냈어요. 내일도 같이 해봐요!"
        }

        return when {
            streak <= 1 -> "시작이 가장 어려운 법이죠. 오늘 잘 하셨어요!"
            streak in 2..4 -> "꾸준함이 힘이에요. 오늘도 한 걸음 전진!"
            streak in 5..9 -> "몸이 확실히 운동을 기억하고 있을 거예요."
            else -> "아주 좋습니다! 이대로만 꾸준히 하면 당신도 몸짱!"
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}