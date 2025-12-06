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
 * 프리셋 리스트 화면 ViewModel.
 *
 * - 프리셋 목록 로드
 * - 새 프리셋 생성
 * - 선택/삭제 처리
 */
@HiltViewModel
class PresetListViewModel @Inject constructor(
    private val presetRepository: PresetRepository,
    private val userStateManager: UserStateManager
) : ViewModel() {

    data class PresetItemUi(
        val id: String,      // UUID
        val name: String,
        val stepCount: Int   // 해당 프리셋의 스텝 개수
    )

    data class PresetListUiState(
        val presets: List<PresetItemUi> = emptyList(),
        val isLoading: Boolean = false,
        val currentPresetId: String? = null,     // 홈에서 이미 선택된 프리셋
        val tempSelectedPresetId: String? = null // 리스트에서 임시로 선택한 프리셋
    )

    private val _uiState = MutableStateFlow(PresetListUiState())
    val uiState: StateFlow<PresetListUiState> = _uiState

    init {
        loadPresets()
    }

    /**
     * DB에서 모든 프리셋 + 스텝을 불러와 UI 모델로 변환한다.
     */
    fun loadPresets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val all = presetRepository.getAllPresetsWithSteps()
            val items = all.map { presetWithSteps ->
                PresetItemUi(
                    id = presetWithSteps.preset.id,
                    name = presetWithSteps.preset.name,
                    stepCount = presetWithSteps.steps.size
                )
            }

            val selectedId = userStateManager.getSelectedPresetId()

            _uiState.update { state ->
                state.copy(
                    presets = items,
                    isLoading = false,
                    currentPresetId = selectedId
                )
            }
        }
    }

    /**
     * 비어있는 새 프리셋을 만들고 즉시 목록을 갱신한다.
     */
    suspend fun createEmptyPreset(defaultName: String = "New Preset"): String {
        val newId = presetRepository.createPreset(
            name = defaultName,
            steps = emptyList()
        )
        // 새 프리셋이 목록에 보이도록 즉시 갱신
        loadPresets()
        return newId
    }

    /**
     * 리스트에서 아이템을 눌렀을 때 '임시 선택' 상태만 업데이트한다.
     */
    fun onPresetClicked(id: String) {
        _uiState.update { state ->
            state.copy(tempSelectedPresetId = id)
        }
    }

    // 프리셋 삭제 버튼
    suspend fun deletePreset(id: String) {
        // 실제 DB 삭제
        presetRepository.deletePreset(id)

        val state = _uiState.value

        val newCurrent = if (state.currentPresetId == id) null else state.currentPresetId
        val newTemp    = if (state.tempSelectedPresetId == id) null else state.tempSelectedPresetId

        _uiState.value = state.copy(
            currentPresetId = newCurrent,
            tempSelectedPresetId = newTemp
        )

        // 삭제 후 목록 새로고침
        loadPresets()
    }

    /**
     * 최종 선택 완료.
     * - tempSelectedPresetId를 실제 선택된 프리셋으로 반영
     */
    fun confirmSelection(
        onNoSelection: () -> Unit,
        onSelected: (String) -> Unit
    ) {
        val tempId = _uiState.value.tempSelectedPresetId

        if (tempId == null) {
            // 아무 것도 고르지 않았으면 알림만 띄우게 콜백
            onNoSelection()
            return
        }

        viewModelScope.launch {
            // 임시 선택된 프리셋을 저장
            userStateManager.selectPreset(tempId)

            // ViewModel 상태에도 이미 선택된 프리셋으로 반영
            _uiState.update { state ->
                state.copy(currentPresetId = tempId)
            }

            // Fragment에 id를 넘겨줌
            onSelected(tempId)
        }
    }
}