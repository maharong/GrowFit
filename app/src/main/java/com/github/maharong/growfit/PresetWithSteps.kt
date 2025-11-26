package com.github.maharong.growfit

import androidx.room.Embedded
import androidx.room.Relation

data class PresetWithSteps(
    @Embedded val preset: PresetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "presetId",
        entity = PresetStepEntity::class
    )
    val steps: List<PresetStepEntity>
)