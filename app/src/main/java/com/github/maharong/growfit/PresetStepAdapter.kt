package com.github.maharong.growfit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.maharong.growfit.databinding.ItemPresetStepBinding
import androidx.core.graphics.toColorInt

/**
 * 프리셋 편집 화면에서 사용하는 스텝 리스트 어댑터.
 *
 * - 아이템 클릭: 스텝 선택 (selectedStepId 갱신용 콜백)
 * - 삭제 버튼 클릭: 해당 스텝 삭제 콜백
 * - 선택된 스텝은 배경 색으로 표시
 */
class PresetStepAdapter(
    private val onItemClick: (PresetStepEntity) -> Unit,
    private val onDeleteClick: (PresetStepEntity) -> Unit
) : ListAdapter<PresetStepEntity, PresetStepAdapter.StepViewHolder>(DiffCallback) {

    /**
     * 현재 선택된 스텝 ID (렌치 버튼으로 편집할 대상)
     */
    var selectedStepId: String? = null
        set(value) {
            val old = field
            field = value
            if (old != value) {
                old?.let { refreshItem(it) }
                value?.let { refreshItem(it) }
            }
        }

    // 특정 ID를 가진 아이템만 갱신
    private fun refreshItem(id: String) {
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPresetStepBinding.inflate(inflater, parent, false)
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val item = getItem(position)
        val isSelected = (item.id == selectedStepId)
        holder.bind(item, isSelected)
    }

    inner class StepViewHolder(
        private val binding: ItemPresetStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(step: PresetStepEntity, isSelected: Boolean) {
            // --- 1) 타이틀: STEP 번호 + 이름/타입 ---

            val displayName = step.name.ifBlank {
                // name 이 비어 있을 때 타입별 기본 표시
                when (step.type) {
                    StepType.TIME -> "TIME"
                    StepType.COUNT -> "COUNT"
                    StepType.REST -> "REST"
                    StepType.WALKING -> "WALK"
                    StepType.RUNNING -> "RUN"
                }
            }

            val stepIndex = step.order + 1
            binding.textStepTitle.text = "STEP $stepIndex · $displayName"

            // --- 2) 상세 정보: duration / count / stepGoal 조합 ---
            // TIME / COUNT / REST / WALKING / RUNNING 모두 공통 로직
            // - TIME / REST : durationSec
            // - COUNT       : count
            // - WALKING/RUNNING : durationSec / stepGoal 둘 다 허용 (있는 것만 표시)

            val detail = when (step.type) {
                StepType.TIME -> {
                    step.durationSec?.takeIf { it > 0 }?.let { sec ->
                        formatSeconds(sec)
                    } ?: "-"
                }
                StepType.COUNT -> {
                    step.count?.takeIf { it > 0 }?.let { c ->
                        "${c}reps"
                    } ?: "-"
                }
                StepType.REST -> {
                    step.durationSec?.takeIf { it > 0 }?.let { sec ->
                        formatSeconds(sec)
                    } ?: "-"
                }
                StepType.WALKING, StepType.RUNNING -> {
                    when {
                        step.stepGoal?.takeIf { it > 0 } != null -> {
                            "${step.stepGoal} steps"
                        }
                        step.durationSec?.takeIf { it > 0 } != null -> {
                            formatSeconds(step.durationSec)
                        }
                        else -> "-"
                    }
                }
            }

            // 선택된 항목이면 SELECTING 붙이기
            val finalDetail =
                if (isSelected) "$detail · SELECTING" else detail

            binding.textStepDetail.text = finalDetail

            // --- 3) 선택 상태에 따른 배경 색 ---

            val card = binding.root

            val normalColor   = "#FFFFFF".toColorInt()
            val selectedColor = "#E0E0E0".toColorInt()

            card.setCardBackgroundColor(
                if (isSelected) selectedColor else normalColor
            )

            // --- 4) 클릭 리스너 ---

            // 전체 아이템 클릭 -> 선택 콜백
            binding.root.setOnClickListener {
                onItemClick(step)
            }

            // 삭제 버튼 클릭 -> 삭제 콜백
            binding.btnDeleteStep.setOnClickListener {
                onDeleteClick(step)
            }
        }
    }

    private fun formatSeconds(sec: Int): String {
        if (sec <= 0) return "-"
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return when {
            h > 0 && m > 0 && s > 0 -> "${h}h ${m}m ${s}s"
            h > 0 && m > 0          -> "${h}h ${m}m"
            h > 0                  -> "${h}h"
            m > 0 && s > 0         -> "${m}m ${s}s"
            m > 0                  -> "${m}m"
            else                   -> "${s}s"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PresetStepEntity>() {
        override fun areItemsTheSame(
            oldItem: PresetStepEntity,
            newItem: PresetStepEntity
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PresetStepEntity,
            newItem: PresetStepEntity
        ): Boolean = oldItem == newItem
    }
}