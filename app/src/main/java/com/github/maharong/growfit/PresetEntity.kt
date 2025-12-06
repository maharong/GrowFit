package com.github.maharong.growfit

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 운동 프리셋의 기본 엔티티.
 *
 * - 이름(name)
 * - 생성 시각 / 마지막 수정 시각
 * - 각 프리셋은 여러 스텝(PresetStepEntity)을 가진다.
 */
@Entity(tableName = "preset")
data class PresetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // UUID
    val name: String, // 프리셋 이름
    val createdAt: Long = System.currentTimeMillis(),// 생성 timestamp
    val updatedAt: Long = System.currentTimeMillis() // 마지막 수정 timestamp
)
