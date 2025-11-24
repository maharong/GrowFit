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
                binding.imagePlant.setImageResource(plantResId)

                // 2. 레벨 텍스트
                binding.textLevel.text = "Lv.${state.level}"

                // 3. 경험치 ProgressBar (현재 레벨 구간 내 위치만 보여줌)
                val minExp = state.minExpForLevel
                val maxExp = state.maxExpForLevel
                val currentExp = state.exp

                // ProgressBar는 "이 레벨 구간 안에서"의 상대값만 보여준다.
                val levelRange = (maxExp - minExp).coerceAtLeast(1)
                val progressWithinLevel = (currentExp - minExp).coerceIn(0, levelRange)

                binding.progressExp.max = levelRange
                binding.progressExp.progress = progressWithinLevel

                // 텍스트는 총 exp / 구간 max 등 원하는 형식대로 표시 가능
                binding.textExp.text = "$currentExp / $maxExp EXP"

                // 4. 포인트 텍스트
                binding.textPoints.text = "${state.points} P"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}