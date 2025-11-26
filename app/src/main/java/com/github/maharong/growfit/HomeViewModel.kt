package com.github.maharong.growfit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 홈 화면에서 사용할 UI 상태를 담는 데이터 클래스.
 * - exp          : 현재 경험치
 * - level        : 경험치로부터 계산된 레벨
 * - points       : 보유 포인트
 * - skinId       : 현재 선택된 스킨 ID
 * - min/maxExp   : 현재 레벨 구간의 최소/최대 경험치
 * - presetName   : 선택된 프리셋 이름 (없으면 null)
 * - todayComplete: 오늘 루틴 완료 여부
 */
data class HomeUiState(
    val exp: Int = 0,
    val level: Int = 1,
    val points: Int = 0,
    val skinId: Int = 0,
    val minExpForLevel: Int = 0,
    val maxExpForLevel: Int = 100,

    val presetName: String? = null,
    val todayComplete: Boolean = false
)

/**
 * 홈 화면용 ViewModel.
 *
 * - UserStateManager를 사용해 현재 유저 상태를 불러오고
 * - 화면에서 필요한 형태(HomeUiState)로 가공해서 StateFlow로 제공한다.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userStateManager: UserStateManager,
    private val presetRepository: PresetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    /**
     * 홈 화면 진입 시 호출.
     * - 비활성 패널티 적용
     * - 현재 상태 불러와서 UI 상태 갱신
     */
    fun load() {
        viewModelScope.launch {
            // 오늘 날짜 기준 TODAY 플래그 초기화
            userStateManager.resetTodayIfNewDay()

            // 운동 안 한 기간이 길다면 패널티 적용
            userStateManager.applyInactivityPenaltyIfNeeded()

            // 현재 상태 로드
            val state = userStateManager.getCurrentState()
            val level = userStateManager.getLevel(state.exp)
            val minExp = userStateManager.getMinExpForLevel(level)
            val maxExp = userStateManager.getMaxExpForLevel(level)
            val presetName = state.selectedPresetId?.let { id ->
                presetRepository.getPresetName(id)
            }

            // 홈 화면에서 사용할 UI 상태 갱신
            _uiState.value = HomeUiState(
                exp = state.exp,
                level = level,
                points = state.points,
                skinId = state.skinId,
                minExpForLevel = minExp,
                maxExpForLevel = maxExp,
                presetName = presetName,
                todayComplete = state.todayComplete
            )
        }
    }

    /**
     * 프리셋(운동) 완료 후 홈 화면으로 돌아왔을 때,
     * 보상 지급 후 상태를 다시 로드하는 함수.
     */
    fun refreshAfterPreset() {
        viewModelScope.launch {
            val state = userStateManager.getCurrentState()
            val level = userStateManager.getLevel(state.exp)
            val minExp = userStateManager.getMinExpForLevel(level)
            val maxExp = userStateManager.getMaxExpForLevel(level)
            val presetName = state.selectedPresetId?.let { id ->
                presetRepository.getPresetName(id)
            }

            _uiState.value = HomeUiState(
                exp = state.exp,
                level = level,
                points = state.points,
                skinId = state.skinId,
                minExpForLevel = minExp,
                maxExpForLevel = maxExp,
                presetName = presetName,
                todayComplete = state.todayComplete
            )
        }
    }
}
