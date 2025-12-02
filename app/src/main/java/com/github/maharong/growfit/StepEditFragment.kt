package com.github.maharong.growfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.maharong.growfit.databinding.FragmentStepEditBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StepEditFragment : Fragment() {

    private var _binding: FragmentStepEditBinding? = null
    private val binding get() = _binding!!

    // PresetEditFragment와 공유
    private val presetEditViewModel: PresetEditViewModel by activityViewModels()

    private lateinit var presetId: String
    private lateinit var stepId: String
    private var isNew: Boolean = false

    private var currentStep: PresetStepEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = StepEditFragmentArgs.fromBundle(requireArguments())
        presetId = args.presetId
        stepId = args.stepId
        isNew = args.isNew
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStepTypeSpinner()
        setupButtons()
        observeUiState()
    }

    private fun setupStepTypeSpinner() {
        val types = StepType.entries.toTypedArray()
        val labels = types.map {
            when (it) {
                StepType.TIME    -> "TIME"
                StepType.COUNT   -> "COUNT"
                StepType.REST    -> "REST"
                StepType.WALKING -> "WALK"
                StepType.RUNNING -> "RUN"
            }
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerStepType.adapter = adapter

        binding.spinnerStepType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val type = StepType.entries[position]
                    updateFieldVisibility(type)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            presetEditViewModel.uiState.collectLatest { state ->
                if (state.presetId != presetId) return@collectLatest

                val step = state.steps.find { it.id == stepId }
                if (step == null) {
                    toast("스텝을 찾을 수 없습니다.")
                    findNavController().popBackStack()
                    return@collectLatest
                }

                if (currentStep == null) {
                    currentStep = step
                    bindStepToUi(step)
                } else {
                    currentStep = step
                }
            }
        }
    }

    private fun bindStepToUi(step: PresetStepEntity) {
        binding.editStepName.setText(step.name)

        // 타입
        val typeIndex = StepType.entries.indexOf(step.type)
        if (typeIndex >= 0) {
            binding.spinnerStepType.setSelection(typeIndex)
        }

        // WALK/RUN 모드: duration이 있으면 TIME 모드, 없고 stepGoal 있으면 STEPS 모드
        if (step.type == StepType.WALKING || step.type == StepType.RUNNING) {
            if (step.stepGoal?.takeIf { it > 0 } != null) {
                binding.radioModeSteps.isChecked = true
            } else {
                binding.radioModeTime.isChecked = true
            }
        } else {
            // 다른 타입에서는 라디오 의미 없음
            binding.radioModeTime.isChecked = true
        }

        // 숫자 필드 세팅
        binding.editDurationSec.setText(step.durationSec?.takeIf { it > 0 }?.toString() ?: "")
        binding.editCount.setText(step.count?.takeIf { it > 0 }?.toString() ?: "")
        binding.editStepGoal.setText(step.stepGoal?.takeIf { it > 0 }?.toString() ?: "")

        updateFieldVisibility(step.type)
    }

    private fun setupButtons() {
        // WALK/RUN 모드 라디오 변경 시 필드 토글
        binding.groupWalkRunMode.setOnCheckedChangeListener { _, _ ->
            val type = StepType.entries[binding.spinnerStepType.selectedItemPosition]
            if (type == StepType.WALKING || type == StepType.RUNNING) {
                updateFieldVisibility(type)
            }
        }

        binding.btnCancelStep.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSaveStep.setOnClickListener {
            saveStep()
        }
    }

    private fun updateFieldVisibility(type: StepType) {
        when (type) {
            StepType.TIME -> {
                binding.groupWalkRunMode.visibility = View.GONE
                showDuration(true)
                showCount(false)
                showStepGoal(false)

                binding.editCount.setText("")
                binding.editStepGoal.setText("")
            }
            StepType.REST -> {
                binding.groupWalkRunMode.visibility = View.GONE
                showDuration(true)
                showCount(false)
                showStepGoal(false)

                binding.editCount.setText("")
                binding.editStepGoal.setText("")
            }
            StepType.COUNT -> {
                binding.groupWalkRunMode.visibility = View.GONE
                showDuration(false)
                showCount(true)
                showStepGoal(false)

                binding.editDurationSec.setText("")
                binding.editStepGoal.setText("")
            }
            StepType.WALKING, StepType.RUNNING -> {
                binding.groupWalkRunMode.visibility = View.VISIBLE
                val useTimeMode = binding.radioModeTime.isChecked

                if (useTimeMode) {
                    // TIME 모드 → duration만
                    showDuration(true)
                    showStepGoal(false)
                    // 서로 배타
                    binding.editStepGoal.setText("")
                } else {
                    // STEPS 모드 → stepGoal만
                    showDuration(false)
                    showStepGoal(true)
                    binding.editDurationSec.setText("")
                }
                showCount(false)
            }
        }
    }

    private fun showDuration(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        binding.textDurationLabel.visibility = v
        binding.editDurationSec.visibility = v
    }

    private fun showCount(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        binding.textCountLabel.visibility = v
        binding.editCount.visibility = v
    }

    private fun showStepGoal(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        binding.textStepGoalLabel.visibility = v
        binding.editStepGoal.visibility = v
    }

    private fun saveStep() {
        val base = currentStep
        if (base == null) {
            toast("스텝 정보가 없습니다.")
            return
        }

        val name = binding.editStepName.text.toString().trim()
        val selectedType = StepType.entries[binding.spinnerStepType.selectedItemPosition]

        val durationSec = binding.editDurationSec.text.toString()
            .toIntOrNull()?.takeIf { it > 0 }
        val count = binding.editCount.text.toString()
            .toIntOrNull()?.takeIf { it > 0 }
        val stepGoal = binding.editStepGoal.text.toString()
            .toIntOrNull()?.takeIf { it > 0 }

        val (finalDuration, finalCount, finalGoal) = when (selectedType) {
            StepType.TIME -> {
                if (durationSec == null) {
                    toast("지속시간을 입력해 주세요.")
                    return
                }
                Triple(durationSec, null, null)
            }

            StepType.REST -> {
                if (durationSec == null) {
                    toast("휴식 시간을 입력해 주세요.")
                    return
                }
                Triple(durationSec, null, null)
            }

            StepType.COUNT -> {
                if (count == null) {
                    toast("횟수를 입력해 주세요.")
                    return
                }
                Triple(null, count, null)
            }

            StepType.WALKING, StepType.RUNNING -> {
                val useTimeMode = binding.radioModeTime.isChecked
                if (useTimeMode) {
                    if (durationSec == null) {
                        toast("지속시간을 입력해 주세요.")
                        return
                    }
                    Triple(durationSec, null, null)
                } else {
                    if (stepGoal == null) {
                        toast("걸음 수를 입력해 주세요.")
                        return
                    }
                    Triple(null, null, stepGoal)
                }
            }
        }

        val updated = base.copy(
            type = selectedType,
            name = name,
            durationSec = finalDuration,
            count = finalCount,
            stepGoal = finalGoal
        )

        presetEditViewModel.updateStep(updated)
        toast("스텝이 수정되었습니다.")
        findNavController().popBackStack()
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
