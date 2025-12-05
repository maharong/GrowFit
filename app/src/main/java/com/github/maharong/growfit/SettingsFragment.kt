package com.github.maharong.growfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.maharong.growfit.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUi()
        setupListeners()
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (!state.isLoaded) return@collectLatest

                // 전체 진동
                if (binding.switchVibrateOverall.isChecked != state.vibrateEnabled) {
                    binding.switchVibrateOverall.isChecked = state.vibrateEnabled
                }

                // 마지막 N초 전 진동 텍스트를 동적으로 설정
                val lastSecondsText = "마지막 ${state.vibrateLastSeconds}초 전 진동"
                if (binding.switchVibrateLastSeconds.text != lastSecondsText) {
                    binding.switchVibrateLastSeconds.text = lastSecondsText
                }

                if (binding.switchVibrateLastSeconds.isChecked != state.vibrateLastSecondsEnabled) {
                    binding.switchVibrateLastSeconds.isChecked = state.vibrateLastSecondsEnabled
                }

                if (binding.switchVibrateStepChange.isChecked != state.vibrateOnStepChange) {
                    binding.switchVibrateStepChange.isChecked = state.vibrateOnStepChange
                }

                if (binding.switchVibratePresetComplete.isChecked != state.vibrateOnPresetComplete) {
                    binding.switchVibratePresetComplete.isChecked = state.vibrateOnPresetComplete
                }

                val currentText = binding.editLastSeconds.text?.toString() ?: ""
                val targetText = state.vibrateLastSeconds.toString()
                if (currentText != targetText) {
                    binding.editLastSeconds.setText(targetText)
                }

                // 전체 진동이 꺼져 있으면 하위 항목 비활성화
                val enabled = state.vibrateEnabled
                binding.switchVibrateLastSeconds.isEnabled = enabled
                binding.switchVibrateStepChange.isEnabled = enabled
                binding.switchVibratePresetComplete.isEnabled = enabled
                binding.editLastSeconds.isEnabled = enabled && state.vibrateLastSecondsEnabled
            }
        }
    }

    private fun setupListeners() {
        binding.switchVibrateOverall.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setVibrateEnabled(isChecked)
        }

        binding.switchVibrateLastSeconds.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setLastSecondsEnabled(isChecked)
        }

        binding.switchVibrateStepChange.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setStepChangeEnabled(isChecked)
        }

        binding.switchVibratePresetComplete.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setPresetCompleteEnabled(isChecked)
        }

        binding.editLastSeconds.addTextChangedListener { editable ->
            val text = editable?.toString().orEmpty()
            val value = text.toIntOrNull()
            if (value != null) {
                viewModel.setLastSeconds(value)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}