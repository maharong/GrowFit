package com.github.maharong.growfit

/**
 * 스킨 ID + 레벨을 조합해
 * 실제 drawable 리소스를 반환하는 매핑 클래스.
 *
 * - skinId → 어떤 스킨 세트를 쓸지
 * - level  → 그 스킨의 몇 단계 이미지인지
 * 스킨 파일 이름은 plant_종류_lv1 이런 식으로 지정한다.
 */
object PlantSkinMapper {
    /**
     * 스킨 ID와 성장 레벨으로 적절한 drawable 리소스를 반환한다.
     * level 은 1~5 범위를 벗어날 수 있으므로 안전하게 보정(coerceIn)한다.
     */
    fun getPlantDrawable(skinId: Int, level: Int): Int {
        val safeLevel = level.coerceIn(1, 5)

        return when (skinId) {
            0 -> defaultSkin(safeLevel)
            // 1 -> cactusSkin(safeLevel)
            else -> defaultSkin(safeLevel) // 정의되지 않은 경우 기본 스킨 사용
        }
    }

    /**
     * 기본 스킨(default) 이미지 매핑
     */
    private fun defaultSkin(level: Int): Int = when (level) {
        1 -> R.drawable.plant_default_lv1
        2 -> R.drawable.plant_default_lv2
        3 -> R.drawable.plant_default_lv3
        4 -> R.drawable.plant_default_lv4
        else -> R.drawable.plant_default_lv5
    }

    /**
     * 선인장(cactus) 스킨
     */
    /**
    private fun cactusSkin(level: Int): Int = when (level) {
        1 -> R.drawable.plant_cactus_lv1
        2 -> R.drawable.plant_cactus_lv2
        3 -> R.drawable.plant_cactus_lv3
        4 -> R.drawable.plant_cactus_lv4
        else -> R.drawable.plant_cactus_lv5
    }
    */
}