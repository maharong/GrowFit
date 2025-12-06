package com.github.maharong.growfit

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * 프리셋(PresetEntity) + 스텝(PresetStepEntity)의
 * CRUD 및 관계 조회(@Relation)를 담당하는 DAO.
 *
 * Repository에서 비즈니스 로직을 처리하고,
 * DAO는 순수 데이터 접근만 수행한다.
 */
@Dao
interface PresetDao {

    /** 프리셋/스텝 생성 */
    @Insert suspend fun insertPreset(preset: PresetEntity)
    @Insert suspend fun insertStep(step: PresetStepEntity)

    /** 프리셋/스텝 수정 */
    @Update suspend fun updatePreset(preset: PresetEntity)
    @Update suspend fun updateStep(step: PresetStepEntity)

    /** 프리셋/스텝 삭제 */
    @Delete suspend fun deletePreset(preset: PresetEntity)
    @Delete suspend fun deleteStep(step: PresetStepEntity)

    /** 특정 프리셋의 모든 스텝 삭제 */
    @Query("DELETE FROM preset_step WHERE presetId = :presetId")
    suspend fun deleteStepsByPresetId(presetId: String)

    /**
     * 프리셋 + 하위 스텝 전체를 하나의 구조로 반환한다.
     * Room의 @Relation 사용.
     */
    @Transaction
    @Query("SELECT * FROM preset WHERE id = :presetId")
    suspend fun getPresetWithSteps(presetId: String): PresetWithSteps?

    /** 전체 프리셋 + 스텝 전체 목록 조회 */
    @Transaction
    @Query("SELECT * FROM preset ORDER BY createdAt ASC")
    suspend fun getAllPresetsWithSteps(): List<PresetWithSteps>
}