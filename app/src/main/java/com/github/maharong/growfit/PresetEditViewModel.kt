package com.github.maharong.growfit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 프리셋 편집 화면의 ViewModel.
 *
 * - 프리셋 로드
 * - 스텝 추가/삭제/수정
 * - 스텝 순서 변경
 * - 전체 저장(save)
 */
@HiltViewModel
class PresetEditViewModel @Inject constructor(
    private val repo: PresetRepository
) : ViewModel() {

    data class PresetEditUiState(
        val presetId: String = "",
        val name: String = "",
        val steps: List<PresetStepEntity> = emptyList(),
        val selectedStepId: String? = null
    )

    private val _uiState = MutableStateFlow(PresetEditUiState())
    val uiState: StateFlow<PresetEditUiState> = _uiState

    // 같은 프리셋을 중복 로딩하지 않도록 상태 유지
    private var loadedPresetId: String? = null

    /**
     * 프리셋 + 스텝 전체 로드 후 UIState로 반영.
     * 이미 로드했던 presetId면 다시 로드하지 않음.
     */
    fun load(presetId: String) {
        // 이미 로딩했던 프리셋은 다시 로드 안함
        if (loadedPresetId == presetId && _uiState.value.presetId == presetId) {
            return
        }

        viewModelScope.launch {
            val data = repo.getPresetWithSteps(presetId) ?: return@launch
            _uiState.value = PresetEditUiState(
                presetId = presetId,
                name = data.preset.name,
                steps = data.steps.sortedBy { it.order }
            )
            loadedPresetId = presetId
        }
    }

    /** 프리셋 이름 변경 */
    fun updateName(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    /** 스텝 수정 */
    fun updateStep(step: PresetStepEntity) {
        _uiState.update { state ->
            state.copy(
                steps = state.steps.map { if (it.id == step.id) step else it }
            )
        }
    }

    /** 스텝 선택 표시 */
    fun selectStep(id: String) {
        _uiState.update { state ->
            state.copy(selectedStepId = id)
        }
    }

    /** 스텝 삭제 후 선택 상태도 정리 */
    fun deleteStep(id: String) {
        _uiState.update { state ->
            val filtered = state.steps.filter { it.id != id }

            val normalized = filtered
                .sortedBy { it.order }
                .mapIndexed { index, step -> step.copy(order = index) }

            val newSelected = if (state.selectedStepId == id) null else state.selectedStepId

            state.copy(
                steps = normalized,
                selectedStepId = newSelected
            )
        }
    }

    /** 새 스텝 추가 */
    fun addStep(newStep: PresetStepEntity) {
        Log.d("PresetEdit", "ADD STEP: presetId=${newStep.presetId}, stepId=${newStep.id}")
        _uiState.update { state ->
            state.copy(steps = state.steps + newStep)
        }
    }

    /**
     * 전체 저장.
     *
     * - 순서(order) 포함해 전달된 스텝 리스트를 그대로 repo에 저장
     */
    suspend fun save() {
        val state = _uiState.value
        repo.savePresetWithSteps(
            presetId = state.presetId,
            name = state.name,
            steps = state.steps
        )
    }

    /**
     * 스텝 순서 이동.
     * UI에서 drag & drop 형태로 재배치할 때 호출된다.
     */
    fun moveStep(from: Int, to: Int) {
        _uiState.update { state ->
            val list = state.steps.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)

            val reOrdered = list.mapIndexed { index, step ->
                step.copy(order = index)
            }

            state.copy(steps = reOrdered)
        }
    }

    /** 다른 프리셋 편집으로 넘어가기 전에 세션 초기화 */
    fun clearSession() {
        loadedPresetId = null
        _uiState.value = PresetEditUiState()
    }
}
