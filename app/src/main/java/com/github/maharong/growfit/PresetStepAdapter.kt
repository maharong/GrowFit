package com.github.maharong.growfit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.maharong.growfit.databinding.ItemPresetStepBinding
import androidx.core.graphics.toColorInt

/**
 * 프리셋 편집 화면에서 스텝 목록을 보여주는 RecyclerView 어댑터.
 *
 * - onItemClick : 스텝 선택(하이라이트)
 * - onDeleteClick : 스텝 삭제
 * - selectedStepId 값에 따라 선택된 항목을 강조 표시
 */
class PresetStepAdapter(
    private val onItemClick: (PresetStepEntity) -> Unit,
    private val onDeleteClick: (PresetStepEntity) -> Unit
) : ListAdapter<PresetStepEntity, PresetStepAdapter.StepViewHolder>(DiffCallback) {

    /** 현재 선택된 스텝 ID */
    var selectedStepId: String? = null
        set(value) {
            val old = field
            field = value
            // 선택 상태가 바뀐 항목만 갱신하여 깜빡임 방지
            if (old != value) {
                old?.let { refreshItem(it) }
                value?.let { refreshItem(it) }
            }
        }

    /** 특정 항목만 새로고침 */
    private fun refreshItem(id: String) {
        val idx = currentList.indexOfFirst { it.id == id }
        if (idx != -1) notifyItemChanged(idx)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPresetStepBinding.inflate(inflater, parent, false)
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, item.id == selectedStepId)
    }

    inner class StepViewHolder(
        private val binding: ItemPresetStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(step: PresetStepEntity, isSelected: Boolean) {
            // 빈 name일 경우 타입 기본 표시
            val displayName = step.name.ifBlank {
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

            // 타입별 상세 정보 선택적으로 표시
            val detail = when (step.type) {
                StepType.TIME, StepType.REST ->
                    step.durationSec?.takeIf { it > 0 }?.let { formatSeconds(it) } ?: "-"

                StepType.COUNT ->
                    step.count?.takeIf { it > 0 }?.let { "${it}reps" } ?: "-"

                StepType.WALKING, StepType.RUNNING ->
                    when {
                        step.stepGoal?.takeIf { it > 0 } != null ->
                            "${step.stepGoal} steps"

                        step.durationSec?.takeIf { it > 0 } != null ->
                            formatSeconds(step.durationSec)

                        else -> "-"
                    }
            }

            // 선택 상태 표시
            binding.textStepDetail.text =
                if (isSelected) "$detail · SELECTING" else detail

            // 배경색 설정
            val normalColor = "#FFFFFF".toColorInt()
            val selectedColor = "#E0E0E0".toColorInt()
            binding.root.setCardBackgroundColor(
                if (isSelected) selectedColor else normalColor
            )

            // 클릭 & 삭제 콜백
            binding.root.setOnClickListener { onItemClick(step) }
            binding.btnDeleteStep.setOnClickListener { onDeleteClick(step) }
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
            h > 0                   -> "${h}h"
            m > 0 && s > 0          -> "${m}m ${s}s"
            m > 0                   -> "${m}m"
            else                    -> "${s}s"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PresetStepEntity>() {
        override fun areItemsTheSame(oldItem: PresetStepEntity, newItem: PresetStepEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PresetStepEntity, newItem: PresetStepEntity): Boolean =
            oldItem == newItem
    }
}