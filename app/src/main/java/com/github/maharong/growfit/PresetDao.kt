package com.github.maharong.growfit

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface PresetDao {

    @Insert suspend fun insertPreset(preset: PresetEntity)
    @Insert suspend fun insertStep(step: PresetStepEntity)

    @Update
    suspend fun updatePreset(preset: PresetEntity)
    @Update
    suspend fun updateStep(step: PresetStepEntity)

    @Delete suspend fun deletePreset(preset: PresetEntity)

    @Transaction
    @Query("SELECT * FROM preset WHERE id = :presetId")
    suspend fun getPresetWithSteps(presetId: String): PresetWithSteps?

    @Transaction
    @Query("SELECT * FROM preset ORDER BY createdAt ASC")
    suspend fun getAllPresetsWithSteps(): List<PresetWithSteps>
}
