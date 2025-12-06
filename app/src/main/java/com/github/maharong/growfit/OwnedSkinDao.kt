package com.github.maharong.growfit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 사용자가 보유한 스킨 목록을 관리하는 DAO.
 *
 * - 보유 스킨 ID 조회
 * - 보유 여부 검사
 * - 스킨 구매(INSERT)
 *
 * REPLACE 전략을 사용하여 같은 스킨을 다시 INSERT해도 문제 없이 덮어쓴다.
 */
@Dao
interface OwnedSkinDao {

    /** 사용자가 보유한 모든 스킨 ID 목록 */
    @Query("SELECT skinId FROM owned_skin")
    suspend fun getAllOwnedSkinIds(): List<Int>

    /** 특정 스킨을 보유하고 있는지 여부 */
    @Query("SELECT EXISTS(SELECT 1 FROM owned_skin WHERE skinId = :skinId)")
    suspend fun isOwned(skinId: Int): Boolean

    /** 스킨 구매(보유 목록에 추가) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OwnedSkinEntity)
}