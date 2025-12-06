package com.github.maharong.growfit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * `UserStateEntity`에 대한 DB 접근을 담당하는 DAO.
 *
 * - 항상 id = 0인 단일 유저 상태만 관리한다.
 * - 상태를 조회하거나, 없으면 생성/갱신한다.
 */
@Dao
interface UserStateDao {

    /**
     * 현재 저장된 유저 상태를 조회한다.
     *
     * @return 존재하면 `UserStateEntity`, 없으면 `null`
     */
    @Query("SELECT * FROM user_state WHERE id = 0 LIMIT 1")
    suspend fun getState(): UserStateEntity?

    /**
     * 유저 상태를 저장한다.
     * 이미 존재하면 갱신하고, 없으면 새로 생성한다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: UserStateEntity)
}