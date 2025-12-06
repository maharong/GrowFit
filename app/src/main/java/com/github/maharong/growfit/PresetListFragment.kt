package com.github.maharong.growfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.isVisible
import com.github.maharong.growfit.databinding.FragmentPresetListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 프리셋 목록 화면.
 *
 * - 프리셋 리스트 표시
 * - 프리셋 선택(임시 → 확정)
 * - 새 프리셋 생성 / 삭제 / 편집 이동
 */
@AndroidEntryPoint
class PresetListFragment : Fragment() {

    private var _binding: FragmentPresetListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PresetListViewModel by viewModels()

    private lateinit var adapter: PresetListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresetListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        observeUiState()
    }

    /** RecyclerView + 어댑터 설정 */
    private fun setupRecyclerView() {
        adapter = PresetListAdapter(
            onItemClick = { item ->
                // 임시 선택만 바꾸고 확정은 따로 진행
                viewModel.onPresetClicked(item.id)
            },
            onDeleteClick = { item ->
                AlertDialog.Builder(requireContext())
                    .setTitle("프리셋 삭제")
                    .setMessage("정말 '${item.name}' 프리셋을 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.deletePreset(item.id)
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        )

        binding.recyclerPresets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PresetListFragment.adapter
        }
    }

    /** 버튼 이벤트 설정 */
    private fun setupButtons() {
        // SELECT → 임시 선택한 프리셋을 실제 선택된 프리셋으로 반영
        binding.btnSelectPreset.setOnClickListener {
            viewModel.confirmSelection(
                onNoSelection = {
                    // 임시 선택이 없을 때 화면 유지 + 안내
                    Toast.makeText(
                        requireContext(),
                        "프리셋을 먼저 선택해 주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onSelected = { _ ->
                    // 선택 확정되면 홈으로 돌아가기
                    findNavController().popBackStack()
                }
            )
        }

        // 새 프리셋 생성 후 편집 화면으로 이동
        binding.btnAddPreset.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val newId = viewModel.createEmptyPreset()
                val action = PresetListFragmentDirections
                    .actionPresetListFragmentToPresetEditFragment(newId)
                findNavController().navigate(action)
            }
        }

        // 임시 선택된 프리셋이 있을 때만 편집 화면으로 이동
        binding.btnEditPreset.setOnClickListener {
            val tempId = viewModel.uiState.value.tempSelectedPresetId
                ?: return@setOnClickListener Toast.makeText(
                    requireContext(), "편집할 프리셋을 먼저 선택해 주세요.", Toast.LENGTH_SHORT
                ).show()

            val action = PresetListFragmentDirections
                .actionPresetListFragmentToPresetEditFragment(tempId)
            findNavController().navigate(action)
        }
    }

    /** ViewModel 상태 구독 → UI 반영 */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // 로딩 스피너 토글
                binding.progressLoading.isVisible = state.isLoading
                binding.recyclerPresets.isVisible = !state.isLoading && state.presets.isNotEmpty()
                binding.textEmpty.isVisible = !state.isLoading && state.presets.isEmpty()

                // 선택 상태를 어댑터에 반영
                adapter.currentPresetId = state.currentPresetId
                adapter.tempSelectedPresetId = state.tempSelectedPresetId

                // 리스트 갱신
                adapter.submitList(state.presets)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 편집 후 복귀 시 목록 갱신
        viewModel.loadPresets()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
