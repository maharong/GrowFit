package com.github.maharong.growfit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.maharong.growfit.databinding.FragmentPresetEditBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PresetEditFragment : Fragment() {

    private var _binding: FragmentPresetEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PresetEditViewModel by activityViewModels()

    private lateinit var adapter: PresetStepAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresetEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val presetIdArg = PresetEditFragmentArgs.fromBundle(requireArguments()).presetId
        val presetId = requireNotNull(presetIdArg) { "presetId is required" }

        setupRecyclerView()
        observeUiState()
        setupButtons()

        viewModel.load(presetId)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (binding.editPresetName.text.toString() != state.name) {
                    binding.editPresetName.setText(state.name)
                }
                adapter.selectedStepId = state.selectedStepId
                adapter.submitList(state.steps)

                val hasSteps = state.steps.isNotEmpty()
                binding.recyclerSteps.visibility = if (hasSteps) View.VISIBLE else View.GONE
                binding.textEmptySteps.visibility = if (hasSteps) View.GONE else View.VISIBLE
            }
        }
    }


    private fun setupRecyclerView() {
        adapter = PresetStepAdapter(
            onItemClick = { step ->
                viewModel.selectStep(step.id)
            },
            onDeleteClick = { step ->
                viewModel.deleteStep(step.id)
            }
        )

        binding.recyclerSteps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PresetEditFragment.adapter
        }

        // 드래그 앤 드롭 연결
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to   = target.bindingAdapterPosition
                viewModel.moveStep(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }

            override fun isLongPressDragEnabled(): Boolean = true
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerSteps)
    }

    private fun setupButtons() {
        // 이름 변경
        binding.editPresetName.addTextChangedListener {
            viewModel.updateName(it.toString())
        }

        // 스텝 추가 버튼
        binding.btnAddStep.setOnClickListener {
            val state = viewModel.uiState.value

            val newStep = PresetStepEntity(
                presetId = state.presetId,
                order = state.steps.size,      // 맨 뒤에 붙이기
                type = StepType.TIME,          // 기본값: 시간 기반
                name = "",
                durationSec = null,
                count = null,
                stepGoal = null
            )

            viewModel.addStep(newStep)

            // 새 스텝의 UUID를 들고 스텝 편집 화면으로 이동
            val action = PresetEditFragmentDirections
                .actionPresetEditFragmentToStepEditFragment(
                    presetId = state.presetId,
                    stepId = newStep.id,
                    isNew = true
                )
            findNavController().navigate(action)
        }

        // 스텝 편집 버튼
        binding.btnEditSteps.setOnClickListener {
            val state = viewModel.uiState.value
            val stepId = state.selectedStepId

            if (stepId == null) {
                Toast.makeText(
                    requireContext(),
                    "편집할 스텝을 먼저 선택해 주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val action = PresetEditFragmentDirections
                .actionPresetEditFragmentToStepEditFragment(
                    presetId = state.presetId,
                    stepId = stepId,
                    isNew = false
                )
            findNavController().navigate(action)
        }

        // 저장 버튼
        binding.btnSavePreset.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    viewModel.save()

                    Toast.makeText(requireContext(), "저장 완료!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    android.util.Log.e("PresetEdit", "SAVE failed", e)
                    Toast.makeText(requireContext(), "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearSession()
    }
}
