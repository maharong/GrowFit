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

    // 어떤 프리셋을 로드했는지 추적
    private var loadedPresetId: String? = null

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

    fun updateName(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun updateStep(step: PresetStepEntity) {
        _uiState.update { state ->
            state.copy(
                steps = state.steps.map { if (it.id == step.id) step else it }
            )
        }
    }

    fun selectStep(id: String) {
        _uiState.update { state ->
            state.copy(selectedStepId = id)
        }
    }

    fun deleteStep(id: String) {
        _uiState.update { state ->
            val newSteps = state.steps.filter { it.id != id }
            val newSelected = if (state.selectedStepId == id) null else state.selectedStepId
            state.copy(
                steps = newSteps,
                selectedStepId = newSelected
            )
        }
    }

    fun addStep(newStep: PresetStepEntity) {
        Log.d("PresetEdit", "ADD STEP: presetId=${newStep.presetId}, stepId=${newStep.id}")
        _uiState.update { state ->
            state.copy(steps = state.steps + newStep)
        }
    }

    suspend fun save() {
        val state = _uiState.value
        repo.savePresetWithSteps(
            presetId = state.presetId,
            name = state.name,
            steps = state.steps
        )
    }

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

    fun clearSession() {
        loadedPresetId = null
        _uiState.value = PresetEditUiState()
    }
}
