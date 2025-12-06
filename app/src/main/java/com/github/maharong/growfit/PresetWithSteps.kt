package com.github.maharong.growfit

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 프리셋(preset)과 그 하위 스텝들을 함께 불러오기 위한 Room 조합 모델.
 *
 * - Room의 @Relation을 사용하여 preset.id → steps.presetId 로 연결한다.
 * - UI / ViewModel에서 프리셋 전체 구조를 한 번에 가져올 때 사용한다.
 */
data class PresetWithSteps(
    @Embedded val preset: PresetEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "presetId",
        entity = PresetStepEntity::class
    )
    val steps: List<PresetStepEntity>
)