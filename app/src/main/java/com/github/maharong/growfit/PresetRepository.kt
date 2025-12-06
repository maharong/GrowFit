package com.github.maharong.growfit

import javax.inject.Inject
import java.util.UUID

/**
 * 프리셋(PresetEntity)과 스텝(PresetStepEntity)에 관한
 * CRUD 및 일괄 저장 로직을 담당하는 Repository.
 *
 * - 프리셋 생성, 조회, 수정, 삭제
 * - 스텝 생성/수정/삭제
 */
class PresetRepository @Inject constructor(
    private val dao: PresetDao
) {

    /**
     * 새 프리셋을 생성하고 스텝을 모두 저장한다.
     *
     * UI에서 order/type/name/durationSec/count/stepGoal만 채워주면 되며
     * id, presetId는 여기서 자동으로 생성한다.
     */
    suspend fun createPreset(name: String, steps: List<PresetStepEntity>): String {
        val now = System.currentTimeMillis()
        val presetId = UUID.randomUUID().toString()

        val preset = PresetEntity(
            id = presetId,
            name = name,
            createdAt = now,
            updatedAt = now
        )
        dao.insertPreset(preset)

        steps.forEachIndexed { index, raw ->
            val step = raw.copy(
                id = UUID.randomUUID().toString(),
                presetId = presetId,
                order = if (raw.order >= 0) raw.order else index
            )
            dao.insertStep(step)
        }

        return presetId
    }

    /** 프리셋 메타데이터만 업데이트할 때 사용 */
    suspend fun updatePreset(preset: PresetEntity) {
        dao.updatePreset(
            preset.copy(updatedAt = System.currentTimeMillis())
        )
    }

    /** 개별 스텝 업데이트 */
    suspend fun updateStep(step: PresetStepEntity) = dao.updateStep(step)

    /** 프리셋 전체 삭제 (CASCADE로 스텝들도 삭제됨) */
    suspend fun deletePreset(presetId: String) {
        dao.getPresetWithSteps(presetId)?.let {
            dao.deletePreset(it.preset)
        }
    }

    /** 프리셋 + 스텝 전체 구조 반환 */
    suspend fun getPresetWithSteps(presetId: String): PresetWithSteps? =
        dao.getPresetWithSteps(presetId)

    /** 전체 프리셋 목록 반환 */
    suspend fun getAllPresetsWithSteps(): List<PresetWithSteps> =
        dao.getAllPresetsWithSteps()

    /** 프리셋 이름만 조회할 때 사용 */
    suspend fun getPresetName(presetId: String): String? =
        dao.getPresetWithSteps(presetId)?.preset?.name

    /**
     * 프리셋 이름 및 스텝 전체를 일괄 저장한다.
     *
     * - 기존 스텝은 update
     * - 새로운 스텝은 insert
     * - UI에서 사라진 스텝은 delete
     */
    suspend fun savePresetWithSteps(
        presetId: String,
        name: String,
        steps: List<PresetStepEntity>
    ) {
        val existing = dao.getPresetWithSteps(presetId) ?: return
        val existingSteps = existing.steps
        val existingIds = existingSteps.map { it.id }.toSet()
        val newIds = steps.map { it.id }.toSet()

        // 프리셋 이름 업데이트
        dao.updatePreset(
            existing.preset.copy(
                name = name,
                updatedAt = System.currentTimeMillis()
            )
        )

        // 신규/기존 스텝 처리
        steps.forEach { s ->
            if (s.id in existingIds) dao.updateStep(s)
            else dao.insertStep(s)
        }

        // 삭제된 스텝 처리
        existingSteps.filter { it.id !in newIds }
            .forEach { dao.deleteStep(it) }
    }
}