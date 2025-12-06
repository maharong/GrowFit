package com.github.maharong.growfit

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자가 보유한 스킨을 저장하는 단일 컬럼 엔티티.
 *
 * - 스킨은 ID 기준으로 구분된다.
 * - 스킨 하나당 Row 하나가 생성된다.
 */
@Entity(tableName = "owned_skin")
data class OwnedSkinEntity(
    @PrimaryKey val skinId: Int
)
