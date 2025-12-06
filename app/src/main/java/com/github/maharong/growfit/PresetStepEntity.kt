package com.github.maharong.growfit

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 하나의 운동 스텝을 나타내는 엔티티.
 *
 * - order: 프리셋 내 순서
 * - type: TIME / COUNT / WALK / RUN 등
 * - durationSec / count / stepGoal 은 타입에 따라 선택적으로 사용
 */
@Entity(
    tableName = "preset_step",
    foreignKeys = [
        ForeignKey(
            entity = PresetEntity::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("presetId")]
)
data class PresetStepEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // UUID
    val presetId: String, // 어떤 프리셋의 스텝인지
    val order: Int, // 프리셋 내 순서
    val type: StepType, // 운동 타입 (운동, 휴식, 걷기/달리기 등)
    val name: String = "", // 스텝 이름 (예: 푸시업, 풀업 등)
    val durationSec: Int?, // 시간 기반 스텝
    val count: Int?, // 횟수 기반 스텝
    val stepGoal: Int? // 걸음 기반 목표 (걷기/달리기)
)

