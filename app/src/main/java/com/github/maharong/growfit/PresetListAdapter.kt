package com.github.maharong.growfit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.maharong.growfit.databinding.ItemPresetBinding
import androidx.core.graphics.toColorInt

/**
 * 프리셋 목록 RecyclerView 어댑터.
 *
 * - currentPresetId : 오늘 선택된 프리셋 강조
 * - tempSelectedPresetId : 리스트에서 임시로 선택한 프리셋 강조
 * - onItemClick : 임시 선택 변경
 * - onDeleteClick : 삭제 요청
 */
class PresetListAdapter(
    private val onItemClick: (PresetListViewModel.PresetItemUi) -> Unit,
    private val onDeleteClick: (PresetListViewModel.PresetItemUi) -> Unit
) : ListAdapter<PresetListViewModel.PresetItemUi, PresetListAdapter.PresetViewHolder>(DiffCallback) {

    // 홈에서 이미 선택되어 있는 프리셋 ID
    var currentPresetId: String? = null
        set(value) {
            val old = field
            field = value
            if (old != value) {
                old?.let { refreshItem(it) }
                value?.let { refreshItem(it) }
            }
        }

    // 리스트 화면에서 임시로 선택된 프리셋 ID
    var tempSelectedPresetId: String? = null
        set(value) {
            val old = field
            field = value
            if (old != value) {
                old?.let { refreshItem(it) }
                value?.let { refreshItem(it) }
            }
        }

    // ID가 일치하는 단일 항목만 갱신
    private fun refreshItem(id: String) {
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPresetBinding.inflate(inflater, parent, false)
        return PresetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        val item = getItem(position)
        val isCurrent = (item.id == currentPresetId)
        val isTemp = (item.id == tempSelectedPresetId)
        holder.bind(item, isCurrent, isTemp)
    }

    inner class PresetViewHolder(
        private val binding: ItemPresetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PresetListViewModel.PresetItemUi, isCurrent: Boolean, isTemp: Boolean) {
            // 이름 / 정보 기본 세팅
            binding.textPresetName.text = item.name

            val baseInfo = "${item.stepCount} steps"
            binding.textPresetInfo.text = when {
                isTemp   -> "$baseInfo · SELECTING"
                else     -> baseInfo
            }

            // 강조 색상 처리
            val card = binding.root
            val normalColor  = "#FFFFFF".toColorInt()   // 기본 흰색
            val currentColor = "#E7D7FF".toColorInt()   // 오늘 프리셋 (밝은 연두색)
            val tempColor    = "#E0E0E0".toColorInt()   // 임시 선택 (회색)

            val bgColor = when {
                isTemp && isCurrent -> currentColor
                isTemp -> tempColor
                isCurrent -> currentColor
                else -> normalColor
            }
            card.setCardBackgroundColor(bgColor)

            // 전체 아이템 클릭 -> 임시 선택 콜백
            binding.root.setOnClickListener {
                onItemClick(item)
            }

            // 삭제 버튼 클릭 -> 삭제 콜백
            binding.btnDeletePreset.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PresetListViewModel.PresetItemUi>() {
        override fun areItemsTheSame(oldItem: PresetListViewModel.PresetItemUi,
                                     newItem: PresetListViewModel.PresetItemUi): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PresetListViewModel.PresetItemUi,
                                        newItem: PresetListViewModel.PresetItemUi): Boolean {
            return oldItem == newItem
        }
    }
}