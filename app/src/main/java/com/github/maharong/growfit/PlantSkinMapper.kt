package com.github.maharong.growfit

/**
 * 스킨 ID + 성장 레벨을 기반으로
 * 실제 표시할 drawable 리소스를 매핑하는 객체.
 *
 * 스킨 ID:
 * - 0 = 기본 스킨
 * - 1 = 선인장 스킨
 *
 * 각 스킨은 lv1~lv5 이미지로 구성되며,
 * level 값이 1~5 범위를 벗어나면 안전하게 보정(coerceIn)한다.
 */
object PlantSkinMapper {
    /**
     * 스킨 ID + 성장 레벨을 받아 drawable 리소스 ID를 반환한다.
     */
    fun getPlantDrawable(skinId: Int, level: Int): Int {
        val safeLevel = level.coerceIn(1, 5)

        return when (skinId) {
            0 -> defaultSkin(safeLevel)
            1 -> cactusSkin(safeLevel)
            else -> defaultSkin(safeLevel) // 정의되지 않은 스킨 ID는 기본 스킨
        }
    }

    /** 기본 스킨 매핑 */
    private fun defaultSkin(level: Int): Int = when (level) {
        1 -> R.drawable.plant_default_lv1
        2 -> R.drawable.plant_default_lv2
        3 -> R.drawable.plant_default_lv3
        4 -> R.drawable.plant_default_lv4
        else -> R.drawable.plant_default_lv5
    }

    /** 선인장 스킨 매핑 */
    private fun cactusSkin(level: Int): Int = when (level) {
        1 -> R.drawable.plant_cactus_lv1
        2 -> R.drawable.plant_cactus_lv2
        3 -> R.drawable.plant_cactus_lv3
        4 -> R.drawable.plant_cactus_lv4
        else -> R.drawable.plant_cactus_lv5
    }
}