package com.github.maharong.growfit

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.maharong.growfit.databinding.FragmentRunBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class RunFragment : Fragment() {

    private var _binding: FragmentRunBinding? = null
    private val binding get() = _binding!!

    // 센서 관련 필드
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var baseStepCount: Int? = null

    // 걸음 목표 자동 넘김이 이미 실행된 스텝인지 체크용
    private var autoMovedStepId: String? = null
    private var lastStepId: String? = null

    // 권한 여부
    private var hasActivityRecognitionPermission: Boolean = false

    private val viewModel: RunViewModel by viewModels()

    companion object {
        // 문자열로 직접 정의해서 API 29 필드 경고 피하기
        private const val PERMISSION_ACTIVITY_RECOGNITION =
            "android.permission.ACTIVITY_RECOGNITION"
    }

    // 활동 인식 권한 요청 (새 Activity Result API 사용)
    private val activityRecognitionPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasActivityRecognitionPermission = granted
            if (!granted && isAdded) {
                Toast.makeText(
                    requireContext(),
                    "걸음 수를 기록하려면 활동 인식 권한이 필요합니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 센서 매니저, 센서 초기화
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // 활동 인식 권한 확인
        checkActivityRecognitionPermission()

        setupBackPressed()
        setupButtons()
        observeUi()
        observeEvents()

        viewModel.start()
    }

    override fun onResume() {
        super.onResume()
        if (hasActivityRecognitionPermission) {
            stepCounterSensor?.also { sensor ->
                sensorManager?.registerListener(
                    stepListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(stepListener)
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitDialog()
                }
            }
        )
    }

    private fun showExitDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("루틴 종료")
            .setMessage("지금 나가면 진행 중인 루틴이 초기화됩니다. 나가시겠습니까?")
            .setPositiveButton("나가기") { _, _ ->
                viewModel.cancelRoutine()
                findNavController().popBackStack()
            }
            .setNegativeButton("계속하기", null)
            .show()
    }

    private fun setupButtons() {
        // 횟수/걸음 기반에서 사용자가 직접 넘길 때
        binding.btnNextStep.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.isFinished) {
                // 완료 상태에서는 홈으로 돌아가기
                findNavController().popBackStack()
            } else {
                // 진행 중일 땐 다음 스텝으로
                viewModel.onUserCompleteCurrentStep()
            }
        }

        // 타이머 스텝 일시정지/재개
        binding.btnPause.setOnClickListener {
            viewModel.togglePause()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->

                if (state.isLoading) {
                    binding.groupContent.visibility = View.GONE
                    binding.progressLoading.visibility = View.VISIBLE
                    return@collect
                } else {
                    binding.progressLoading.visibility = View.VISIBLE
                    binding.groupContent.visibility = View.VISIBLE
                    binding.progressLoading.visibility = View.GONE
                }

                binding.textPresetName.text = state.presetName

                val step = state.currentStep
                if (step == null) {
                    // 이미 끝난 상태
                    binding.groupRunMode.visibility = View.GONE
                    binding.layoutComplete.visibility = View.VISIBLE

                    binding.textCompleteTitle.text = "운동 완료!"
                    binding.textCompleteRewardMessage.text = state.rewardMessage
                    binding.textCompleteStreak.text = state.streakMessage
                    binding.textCompleteComment.text = state.commentMessage

                    baseStepCount = null

                    // 완료 상태에서는 완료 버튼만 보여주기
                    binding.btnPause.visibility = View.GONE
                    binding.btnNextStep.visibility = View.VISIBLE
                    binding.btnNextStep.isEnabled = true
                    binding.btnNextStep.text = "홈으로"

                    return@collect
                } else {
                    binding.groupRunMode.visibility = View.VISIBLE
                    binding.layoutComplete.visibility = View.GONE
                }

                // 스텝이 바뀔 때마다 자동 넘김 플래그 초기화
                if (step.id != lastStepId) {
                    lastStepId = step.id
                    autoMovedStepId = null
                }

                val total = state.steps.size
                val index = state.currentIndex + 1

                // STEP 인덱스, 현재 스텝 이름
                binding.textStepTitle.text = "STEP $index / $total"
                val stepName = step.name.ifBlank { step.type.name }
                binding.textStepName.text = stepName

                // 다음 스텝 안내
                val nextStep = state.steps.getOrNull(state.currentIndex + 1)
                binding.textNextStepInfo.text = if (nextStep == null) {
                    "마지막 스텝입니다."
                } else {
                    val nextName = nextStep.name.ifBlank { nextStep.type.name }
                    "NEXT: $nextName"
                }

                // 타입에 따라 UI 구성
                when (step.type) {
                    StepType.TIME, StepType.REST -> {
                        // 걸음 관련 리셋
                        baseStepCount = null
                        binding.textWalkInfo.visibility = View.GONE

                        binding.textTimer.visibility = View.VISIBLE
                        binding.textCountInfo.visibility = View.GONE

                        val remain = state.remainingSeconds ?: step.durationSec ?: 0
                        binding.textTimer.text = formatSeconds(remain)

                        // 시간 기반은 자동 진행 → next 버튼 숨김, pause 버튼 사용
                        binding.btnNextStep.visibility = View.GONE
                        binding.btnPause.visibility = View.VISIBLE
                        binding.btnPause.text = if (state.isPaused) "재개" else "일시정지"

                        binding.textGuide.text = "마지막 ${5}초 전에 진동으로 알려드려요."
                    }

                    StepType.COUNT -> {
                        // 걸음 관련 리셋
                        baseStepCount = null
                        binding.textWalkInfo.visibility = View.GONE

                        binding.textTimer.visibility = View.GONE
                        binding.textCountInfo.visibility = View.VISIBLE

                        val target = step.count ?: 0
                        binding.textCountInfo.text = "목표 횟수: ${target}회"

                        // 사용자가 직접 완료 버튼 클릭
                        binding.btnPause.visibility = View.GONE
                        binding.btnNextStep.visibility = View.VISIBLE
                        binding.btnNextStep.isEnabled = true
                        binding.btnNextStep.text = "다음 스텝으로"

                        binding.textGuide.text = "동작을 끝낸 뒤 '다음 스텝으로' 버튼을 눌러주세요."
                    }

                    StepType.WALKING, StepType.RUNNING -> {
                        // 걷기/달리기 스텝에 들어올 때마다 기준값 초기화
                        baseStepCount = null

                        binding.textCountInfo.visibility = View.GONE

                        val duration = step.durationSec ?: 0
                        if (duration > 0) {
                            val remain = state.remainingSeconds ?: duration
                            binding.textTimer.visibility = View.VISIBLE
                            binding.textTimer.text = formatSeconds(remain)

                            // 시간 기반 걷기/달리기 : TIME/REST와 동일하게 자동 진행
                            binding.btnNextStep.visibility = View.GONE
                        } else {
                            // 시간 설정이 없으면 타이머는 숨기고, 수동으로 넘기기
                            binding.textTimer.visibility = View.GONE
                            binding.btnNextStep.visibility = View.VISIBLE
                            binding.btnNextStep.isEnabled = true
                            binding.btnNextStep.text = "다음 스텝으로"
                        }

                        // 걸음 정보 표시 (목표가 있으면 목표 + 현재, 없으면 현재만)
                        val goal = step.stepGoal ?: 0
                        val current = state.currentStepsTaken

                        binding.textWalkInfo.visibility = View.VISIBLE
                        binding.textWalkInfo.text = if (goal > 0) {
                            "$current / $goal 보"
                        } else {
                            "현재 : $current 보"
                        }

                        binding.btnPause.visibility = View.GONE
                        binding.textGuide.text = "걷거나 뛰면 걸음 수가 자동으로 기록돼요."
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is RunViewModel.RunEvent.PreAlert -> {
                        vibrate()
                    }
                    is RunViewModel.RunEvent.RoutineFinished -> {
                        vibrateFinishPattern()
                    }
                    is RunViewModel.RunEvent.Error -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requireContext().getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+
            val effect = VibrationEffect.createOneShot(
                300L,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator.vibrate(effect)
        } else {
            // API 25 이하
            @Suppress("DEPRECATION")
            vibrator.vibrate(300L)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateFinishPattern() {
        try {
            val v = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 패턴: 0ms 대기 → 200ms 진동 → 100ms 쉬고 → 300ms 진동 → 100ms 쉬고 → 500ms 진동
                val timings = longArrayOf(0, 200, 100, 300, 100, 500)
                val amplitudes = intArrayOf(
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 200, 100, 300, 100, 500), -1)
            }
        } catch (_: Exception) {}
    }

    private fun formatSeconds(sec: Int): String {
        if (sec <= 0) return "00:00"
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return when {
            h > 0 -> String.format(Locale.US, "%d:%02d:%02d", h, m, s)
            else -> String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager?.unregisterListener(stepListener)
        _binding = null
    }

    // 센서 리스너
    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val sensorEvent = event ?: return
            if (sensorEvent.sensor.type != Sensor.TYPE_STEP_COUNTER) return

            // 현재 스텝이 걷기/달리기일 때만 반응
            val currentStep = viewModel.uiState.value.currentStep ?: return
            if (currentStep.type != StepType.WALKING && currentStep.type != StepType.RUNNING) {
                return
            }

            val totalFromBoot = sensorEvent.values[0].toInt()

            // 아직 기준이 없으면 지금 값을 기준으로 저장
            val base = baseStepCount ?: run {
                baseStepCount = totalFromBoot
                return
            }

            val diff = totalFromBoot - base
            if (diff < 0) return

            // ViewModel에 현재 스텝에서의 걸음 수 전달
            viewModel.updateStepCount(diff)

            val goal = currentStep.stepGoal
            val duration = currentStep.durationSec ?: 0

            // duration이 0 이하이고, goal 이 1 이상이면 걸음 수 모드
            val isStepsMode = duration <= 0 && goal != null && goal > 0

            if (isStepsMode &&
                diff >= goal &&
                autoMovedStepId != currentStep.id
            ) {
                // 이 스텝에서 자동 넘김은 한 번만 실행
                autoMovedStepId = currentStep.id
                viewModel.onUserCompleteCurrentStep()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // 필요 없음
        }
    }

    private fun checkActivityRecognitionPermission() {
        // API 29 미만에서는 ACTIVITY_RECOGNITION 권한이 의미 없으므로 스킵
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            hasActivityRecognitionPermission = true
            return
        }

        hasActivityRecognitionPermission =
            ContextCompat.checkSelfPermission(
                requireContext(),
                PERMISSION_ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasActivityRecognitionPermission) {
            activityRecognitionPermissionLauncher.launch(PERMISSION_ACTIVITY_RECOGNITION)
        }
    }
}