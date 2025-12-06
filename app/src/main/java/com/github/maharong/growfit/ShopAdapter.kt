package com.github.maharong.growfit

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * 스킨 상점 리스트를 표시하는 RecyclerView 어댑터.
 *
 * - 각 아이템에서 스킨 이름/가격/버튼 상태를 보여준다.
 * - 일정 주기마다 레벨 이미지를 바꿔가며 프리뷰 애니메이션을 재생한다.
 */
class ShopAdapter(
    private val onClickSkin: (Int) -> Unit
) : ListAdapter<SkinUiModel, ShopAdapter.SkinViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skin, parent, false)
        return SkinViewHolder(view, onClickSkin)
    }

    override fun onBindViewHolder(holder: SkinViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: SkinViewHolder) {
        super.onViewRecycled(holder)
        // 재활용 시 애니메이션 루프를 중단하여 불필요한 Handler 콜백을 막는다.
        holder.stopLevelAnimation()
    }

    /**
     * 개별 스킨 아이템을 표현하는 ViewHolder.
     */
    class SkinViewHolder(
        itemView: View,
        private val onClickSkin: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgPreview: ImageView = itemView.findViewById(R.id.imgSkinPreview)
        private val txtName: TextView = itemView.findViewById(R.id.txtSkinName)
        private val txtPrice: TextView = itemView.findViewById(R.id.txtSkinPrice)
        private val btnAction: Button = itemView.findViewById(R.id.btnSkinAction)

        private val handler = Handler(Looper.getMainLooper())
        private var currentLevel = 1
        private var currentSkinId: Int = 0

        /**
         * 일정 주기마다 레벨을 1~5까지 순환시키며
         * 해당 레벨의 스킨 이미지를 보여주는 Runnable.
         */
        private val levelRunnable = object : Runnable {
            override fun run() {
                // 레벨 증가 (1~5 반복)
                currentLevel = (currentLevel % 5) + 1

                val drawableRes = PlantSkinMapper.getPlantDrawable(currentSkinId, currentLevel)
                imgPreview.setImageResource(drawableRes)

                // n초마다 반복 (예: 1초)
                handler.postDelayed(this, 1000L)
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: SkinUiModel) {
            currentSkinId = item.id
            currentLevel = 1

            txtName.text = item.name

            // 버튼 및 가격/상태 텍스트 설정
            when (item.buttonState) {
                SkinButtonState.BUY -> {
                    txtPrice.text = "${item.price}P"
                    btnAction.isEnabled = true
                    btnAction.alpha = 1.0f
                    btnAction.text = "구매"
                }
                SkinButtonState.SELECT -> {
                    txtPrice.text = "보유중 · ${item.price}P"
                    btnAction.isEnabled = true
                    btnAction.alpha = 1.0f
                    btnAction.text = "선택"
                }
                SkinButtonState.SELECTED -> {
                    txtPrice.text = "현재 적용중"
                    btnAction.isEnabled = false
                    btnAction.alpha = 0.6f
                    btnAction.text = "적용중"
                }
            }

            // 버튼 클릭 → 상위로 skinId 전달
            btnAction.setOnClickListener {
                onClickSkin(item.id)
            }

            // 즉시 레벨 1 이미지 표시 후 애니메이션 시작
            val firstDrawable = PlantSkinMapper.getPlantDrawable(currentSkinId, currentLevel)
            imgPreview.setImageResource(firstDrawable)

            startLevelAnimation()
        }

        /**
         * 레벨 애니메이션을 시작한다.
         * (기존 콜백을 지우고 1초 후부터 루프 시작)
         */
        private fun startLevelAnimation() {
            stopLevelAnimation()
            handler.postDelayed(levelRunnable, 1000L) // 1초 후부터 시작
        }

        /**
         * 레벨 애니메이션을 중단한다.
         */
        fun stopLevelAnimation() {
            handler.removeCallbacks(levelRunnable)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SkinUiModel>() {
            override fun areItemsTheSame(oldItem: SkinUiModel, newItem: SkinUiModel): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SkinUiModel, newItem: SkinUiModel): Boolean =
                oldItem == newItem
        }
    }
}