package com.github.maharong.growfit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * UserStateEntity에 대한 DB 접근을 담당하는 DAO
 *
 * getState() : 상태값을 불러온다.
 * insertOrUpdate() : 존재하면 갱신하고, 없으면 생성
 */
@Dao
interface UserStateDao {
    @Query("SELECT * FROM user_state WHERE id = 0 LIMIT 1")
    suspend fun getState(): UserStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: UserStateEntity)
}