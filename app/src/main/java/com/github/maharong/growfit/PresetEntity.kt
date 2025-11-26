package com.github.maharong.growfit

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "preset")
data class PresetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // UUID
    val name: String, // 프리셋 이름
    val createdAt: Long = System.currentTimeMillis(),// 생성 timestamp
    val updatedAt: Long = System.currentTimeMillis() // 마지막 수정 timestamp
)
