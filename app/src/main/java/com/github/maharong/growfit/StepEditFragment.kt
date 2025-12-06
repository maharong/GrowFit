package com.github.maharong.growfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.maharong.growfit.databinding.FragmentStepEditBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 프리셋의 개별 스텝(TIME/COUNT/WALK/RUN 등)을 편집하는 화면.
 *
 * - PresetEditFragment와 ViewModel을 공유한다.
 * - 스텝 타입에 따라 사용 가능한 필드를 다르게 보여준다.
 */
@AndroidEntryPoint
class StepEditFragment : Fragment() {

    private var _binding: FragmentStepEditBinding? = null
    private val binding get() = _binding!!

    // PresetEditFragment와 공유하는 ViewModel
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

        // 시스템 뒤로가기 버튼을 눌렀을 때 새 스텝이면 경고 다이얼로그를 띄운다.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isNew) {
                        showExitWarningDialog()
                    } else {
                        findNavController().popBackStack()
                    }
                }
            }
        )

        setupStepTypeSpinner()
        setupButtons()
        observeUiState()
    }

    /**
     * 스텝 타입 선택 스피너를 초기화하고,
     * 선택된 타입에 따라 필드 표시를 갱신한다.
     */
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

    /**
     * PresetEditViewModel의 상태를 구독하고,
     * 현재 편집 중인 스텝 데이터를 UI에 반영한다.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            presetEditViewModel.uiState.collectLatest { state ->
                if (state.presetId != presetId) return@collectLatest

                val step = state.steps.find { it.id == stepId }
                if (step == null) {
                    // 새 스텝인 경우에는 의도적으로 삭제한 것일 수 있으므로 조용히 종료
                    if (!isNew) {
                        toast("스텝을 찾을 수 없습니다.")
                        findNavController().popBackStack()
                    }
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

    /**
     * 새 스텝 편집 도중 나갈 때 경고 다이얼로그를 보여준다.
     */
    private fun showExitWarningDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("스텝 편집 종료")
            .setMessage("입력한 내용이 저장되지 않을 수 있습니다.\n정말 나가시겠습니까?")
            .setPositiveButton("나가기") { _, _ ->
                // 새 스텝이면 삭제
                presetEditViewModel.deleteStep(stepId)
                findNavController().popBackStack()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /**
     * 스텝 엔티티 값을 화면 입력 필드에 채운다.
     */
    private fun bindStepToUi(step: PresetStepEntity) {
        binding.editStepName.setText(step.name)

        // 타입 선택
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

    /**
     * 버튼 및 라디오 그룹 리스너를 설정한다.
     */
    private fun setupButtons() {
        // WALK/RUN 모드에서 TIME/STEPS 라디오 체크 변경 시 필드 토글
        binding.groupWalkRunMode.setOnCheckedChangeListener { _, _ ->
            val type = StepType.entries[binding.spinnerStepType.selectedItemPosition]
            if (type == StepType.WALKING || type == StepType.RUNNING) {
                updateFieldVisibility(type)
            }
        }

        binding.btnCancelStep.setOnClickListener {
            if (isNew) {
                showExitWarningDialog()
            } else {
                findNavController().popBackStack()
            }
        }

        binding.btnSaveStep.setOnClickListener {
            saveStep()
        }
    }

    /**
     * 스텝 타입에 따라 어떤 입력 필드를 보여줄지 결정한다.
     */
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
                    // TIME 모드 → duration만 사용
                    showDuration(true)
                    showStepGoal(false)
                    binding.editStepGoal.setText("")
                } else {
                    // STEPS 모드 → stepGoal만 사용
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

    /**
     * 현재 입력값을 검증하고, ViewModel에 스텝 업데이트를 요청한다.
     */
    private fun saveStep() {
        val base = currentStep
        if (base == null) {
            toast("스텝 정보가 없습니다.")
            return
        }

        val name = binding.editStepName.text.toString().trim()
        val selectedType = StepType.entries[binding.spinnerStepType.selectedItemPosition]

        val (finalDuration, finalCount, finalGoal) =
            when (selectedType) {
                StepType.TIME -> {
                    val durationSec = parsePositiveInt(
                        binding.editDurationSec.text.toString(),
                        "지속시간"
                    ) ?: return
                    Triple(durationSec, null, null)
                }

                StepType.REST -> {
                    val durationSec = parsePositiveInt(
                        binding.editDurationSec.text.toString(),
                        "휴식 시간"
                    ) ?: return
                    Triple(durationSec, null, null)
                }

                StepType.COUNT -> {
                    val count = parsePositiveInt(
                        binding.editCount.text.toString(),
                        "횟수"
                    ) ?: return
                    Triple(null, count, null)
                }

                StepType.WALKING, StepType.RUNNING -> {
                    val useTimeMode = binding.radioModeTime.isChecked
                    if (useTimeMode) {
                        val durationSec = parsePositiveInt(
                            binding.editDurationSec.text.toString(),
                            "지속시간"
                        ) ?: return
                        Triple(durationSec, null, null)
                    } else {
                        val stepGoal = parsePositiveInt(
                            binding.editStepGoal.text.toString(),
                            "걸음 수"
                        ) ?: return
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

        // 한 번이라도 저장이 성공하면, 이 프래그먼트 생명주기 안에서는 새 스텝이 아니라고 본다.
        isNew = false

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

    /**
     * 입력 문자열을 양의 정수로 파싱한다.
     *
     * @param input 입력값
     * @param fieldName 에러 메시지에 표시할 필드 이름
     * @return 1 이상인 정수, 잘못된 입력이면 null
     */
    private fun parsePositiveInt(input: String, fieldName: String): Int? {
        val value = input.toIntOrNull()
        if (value == null || value < 1) {
            toast("$fieldName 은(는) 최소 1 이상이어야 합니다.")
            return null
        }
        return value
    }
}