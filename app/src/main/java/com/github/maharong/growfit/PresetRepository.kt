package com.github.maharong.growfit

import javax.inject.Inject
import java.util.UUID

class PresetRepository @Inject constructor(
    private val dao: PresetDao
) {

    /**
     * 새 프리셋 + 스텝 생성.
     *
     * @param name 프리셋 이름
     * @param steps UI에서 구성한 스텝 정보
     *              - id, presetId는 여기서 채우므로
     *                order, type, name(스텝 이름), durationSec, count, stepGoal만 신경쓰면 됨
     * @return 생성된 프리셋의 UUID (id)
     */
    suspend fun createPreset(
        name: String,
        steps: List<PresetStepEntity>
    ): String {
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
                // raw.order가 이미 유효하면 그걸, 아니면 index 사용
                order = if (raw.order >= 0) raw.order else index
            )
            dao.insertStep(step)
        }

        return presetId
    }

    /**
     * 프리셋 이름 변경 등 헤더만 업데이트할 때 사용.
     */
    suspend fun updatePreset(preset: PresetEntity) {
        val updated = preset.copy(
            updatedAt = System.currentTimeMillis()
        )
        dao.updatePreset(updated)
    }

    /**
     * 개별 스텝 수정.
     */
    suspend fun updateStep(step: PresetStepEntity) {
        dao.updateStep(step)
    }

    /**
     * 프리셋 전체 삭제 (스텝은 CASCADE로 함께 삭제).
     */
    suspend fun deletePreset(presetId: String) {
        val presetWithSteps = dao.getPresetWithSteps(presetId) ?: return
        dao.deletePreset(presetWithSteps.preset)
    }

    /**
     * 지정된 프리셋(id)을 조회하고,
     * 해당 프리셋에 속한 모든 스텝을 함께 가져온다.
     *
     * @return PresetWithSteps(preset = 프리셋 1개, steps = 그 프리셋의 모든 스텝 목록)
     */
    suspend fun getPresetWithSteps(presetId: String): PresetWithSteps? =
        dao.getPresetWithSteps(presetId)

    /**
     * 모든 프리셋 + 스텝 목록.
     */
    suspend fun getAllPresetsWithSteps(): List<PresetWithSteps> =
        dao.getAllPresetsWithSteps()

    /**
     * 홈 화면 등에서 특정 프리셋 이름만 가져올 때 씀.
     */
    suspend fun getPresetName(presetId: String): String? =
        dao.getPresetWithSteps(presetId)?.preset?.name

    /**
     * 프리셋 이름 및 스텝 전체를 동기화.
     */
    suspend fun savePresetWithSteps(
        presetId: String,
        name: String,
        steps: List<PresetStepEntity>
    ) {
        // 기존 프리셋 + 스텝 로드
        val existing = dao.getPresetWithSteps(presetId) ?: return
        val existingSteps = existing.steps
        val existingIds = existingSteps.map { it.id }.toSet()
        val newIds = steps.map { it.id }.toSet()

        // 프리셋 이름/업데이트 시간 갱신
        dao.updatePreset(
            existing.preset.copy(
                name = name,
                updatedAt = System.currentTimeMillis()
            )
        )

        // 새로 추가된 스텝은 insert, 기존 스텝은 update
        steps.forEach { step ->
            if (step.id in existingIds) {
                dao.updateStep(step)
            } else {
                dao.insertStep(step)
            }
        }

        // UI에서 사라진 스텝은 delete
        val toDelete = existingSteps.filter { it.id !in newIds }
        toDelete.forEach { dao.deleteStep(it) }
    }
}