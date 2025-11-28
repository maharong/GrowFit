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
 * 프리셋 리스트 화면용 ViewModel.
 *
 * - 모든 프리셋 목록을 불러와서 UI에 제공
 * - 새 프리셋 생성 (빈 스텝 리스트) 후, 생성된 id를 반환
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
     * 모든 프리셋 + 스텝을 불러와서
     * 리스트 형태로 UI 상태에 반영.
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

            _uiState.update { state ->
                state.copy(
                    presets = items,
                    isLoading = false
                )
            }
        }
    }

    /**
     * 빈 프리셋을 하나 생성하고, 생성된 프리셋의 id(UUID)를 반환.
     *
     * - 이름은 임시로 defaultName 사용
     * - 스텝은 비어 있는 상태에서 시작하고,
     *   이후 PresetEditFragment에서 스텝을 추가/수정한다.
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

    // 프리셋 임시 선택
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
     * SELECT 버튼 눌렀을 때 호출:
     * - tempSelectedPresetId가 있으면 선택된 프리셋으로 확정 + DB에 저장
     * - 없으면 → onNoSelection 콜백만 호출
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