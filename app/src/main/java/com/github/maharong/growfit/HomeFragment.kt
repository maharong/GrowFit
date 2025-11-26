package com.github.maharong.growfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.maharong.growfit.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 홈 화면 Fragment.
 *
 * - HomeViewModel의 uiState를 관찰해서
 *   식물 이미지, 레벨, 경험치, 포인트를 화면에 반영한다.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * View 생성 후, ViewModel 상태를 구독하고 초기 로드를 수행한다.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUiState()
        setupButtons()
        viewModel.load()
    }

    /**
     * ViewModel의 uiState를 관찰하면서
     * 식물 이미지 / 레벨 / 경험치 / 포인트를 UI에 반영한다.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->

                // 1. 스킨 + 레벨 기반 식물 이미지 설정
                val plantResId = PlantSkinMapper.getPlantDrawable(
                    skinId = state.skinId,
                    level = state.level
                )
                binding.imgPlant.setImageResource(plantResId)

                // 2. 레벨 텍스트
                binding.txtLevel.text = "Lv ${state.level}"

                // 3. 경험치 ProgressBar (현재 레벨 구간 내 위치만 보여줌)
                val minExp = state.minExpForLevel
                val maxExp = state.maxExpForLevel
                val currentExp = state.exp

                // ProgressBar는 "이 레벨 구간 안에서"의 상대값만 보여준다.
                val range = (maxExp - minExp).coerceAtLeast(1)
                val progress = (currentExp - minExp).coerceIn(0, range)

                binding.expProgress.max = range
                binding.expProgress.progress = progress

                // EXP 숫자 텍스트
                binding.txtExpValue.text = "$currentExp / $maxExp EXP"

                // 4. 포인트 텍스트
                binding.txtPoints.text = "${state.points} P"

                // 5. 프리셋 + 오늘 운동 상태
                val preset = state.presetName ?: "-"
                val today = if (state.todayComplete) "COMPLETE" else "READY"

                binding.txtPresetStatus.text =
                    "PRESET: $preset\nTODAY: $today"
            }
        }
    }

    private fun setupButtons() {

        // 프리셋 선택
        binding.btnPreset.setOnClickListener {
            // TODO: NavController 연결
        }

        // 운동 시작
        binding.btnStart.setOnClickListener {
            // TODO: 운동 타이머 화면 이동
        }

        // SHOP (스킨 구매)
        binding.btnShop.setOnClickListener {
            // TODO: 상점 화면 이동
        }

        // 설정
        binding.btnSettings.setOnClickListener {
            // TODO: 설정 화면 이동
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}