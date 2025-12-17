package com.github.maharong.growfit

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.room.withTransaction

/**
 * 유저 상태(경험치, 포인트, 운동 날짜, 스킨 보유 등)에 대한
 * 비즈니스 로직을 담당하는 매니저 클래스.
 *
 * - DB 접근은 [UserStateRepository]가 담당한다.
 * - 날짜 계산은 [dateProvider]를 통해 주입받아 테스트하기 쉽게 구성한다.
 */
class UserStateManager(
    private val db: AppDatabase,
    private val repo: UserStateRepository,
    private val ownedSkinRepo: OwnedSkinRepository,
    private val dateProvider: () -> LocalDate = { LocalDate.now() }
) {
    /** 레벨별 최소 경험치 구간. 인덱스 = (레벨 - 1). */
    private val expTable = listOf(0, 100, 250, 500, 900)

    /** 레벨별 보상량을 나타내는 데이터 클래스. */
    data class LevelReward(val exp: Int, val points: Int)

    /** 프리셋 완료 처리 결과를 담는 데이터 클래스. */
    data class PresetCompleteResult(
        val rewardExp: Int,
        val rewardPoints: Int,
        val alreadyReceived: Boolean,
        val streakDays: Int
    )

    /** 스킨 상점 화면에서 사용할 유저 상태. */
    data class SkinShopState(
        val currentSkinId: Int,
        val ownedSkinIds: Set<Int>,
        val points: Int
    )

    /**
     * 현재 레벨에 따른 보상량을 계산한다.
     *
     * @param level 현재 레벨
     * @return 해당 레벨에서 지급할 경험치/포인트 정보
     */
    private fun getRewardForLevel(level: Int): LevelReward {
        return when(level) {
            1 -> LevelReward(exp = 50, points = 10)
            2 -> LevelReward(exp = 60, points = 12)
            3 -> LevelReward(exp = 70, points = 14)
            4 -> LevelReward(exp = 90, points = 18)
            5 -> LevelReward(exp = 0, points = 20) // MAX 레벨에서 포인트만 지급
            else -> LevelReward(exp = 0, points = 0)
        }
    }

    /**
     * 누적 경험치로부터 현재 레벨을 계산한다.
     *
     * @param exp 누적 경험치
     * @return 계산된 레벨(1 이상)
     */
    fun getLevel(exp: Int): Int {
        for (i in expTable.indices.reversed()) {
            if (exp >= expTable[i]) return i + 1
        }
        return 1
    }

    /**
     * 현재 저장된 유저 상태를 로드한다.
     */
    suspend fun getCurrentState(): UserStateEntity {
        return repo.load()
    }

    /**
     * 현재 선택된 프리셋 ID를 반환한다.
     *
     * @return 선택된 프리셋 ID, 없으면 `null`
     */
    suspend fun getSelectedPresetId(): String? {
        return repo.load().selectedPresetId
    }

    /**
     * 프리셋을 완료했을 때 경험치, 포인트, 스트릭을 갱신한다.
     *
     * - 같은 날 두 번 이상 완료되면 보상을 중복 지급하지 않는다.
     * - 연속 운동일(streak)을 계산하고, 필요 시 리셋한다.
     *
     * @return 보상 정보 및 스트릭 정보를 담은 [PresetCompleteResult]
     */
    suspend fun onPresetCompleted(): PresetCompleteResult {
        val todayDate = dateProvider()
        val today = localDateToLong(todayDate)
        val state = repo.load()

        // 오늘 이미 보상을 받았다면 중복 지급하지 않는다.
        if (state.lastWorkoutDay == today) {
            return PresetCompleteResult(
                rewardExp = 0,
                rewardPoints = 0,
                alreadyReceived = true,
                streakDays = state.streakDays
            )
        }

        // 연속 운동일 계산
        val lastWorkout = if (state.lastWorkoutDay == 0L) null else longToLocalDate(state.lastWorkoutDay)
        val newStreak = if (lastWorkout == null) {
            1
        } else {
            val diff = ChronoUnit.DAYS.between(lastWorkout, todayDate).toInt()
            when {
                diff == 1 -> state.streakDays + 1   // 어제도 했다 → 스트릭 +1
                diff <= 0 -> state.streakDays       // 같은 날 두 번 호출 등 이상 케이스 → 유지
                else      -> 1                      // 하루 이상 쉬었으면 스트릭 리셋
            }
        }

        // 현재 레벨 및 리워드 계산
        val level = getLevel(state.exp)
        val reward = getRewardForLevel(level)

        // 오늘 운동 완료 상태로 만들고 마지막 운동 날짜 갱신
        state.todayComplete = true
        state.lastWorkoutDay = today
        state.streakDays = newStreak

        // 보상 지급
        state.exp += reward.exp
        state.points += reward.points

        // 경험치가 MAX 레벨 요구량을 넘었을 경우 상한선으로 고정
        val maxExpCap = expTable.last()
        if (state.exp > maxExpCap) {
            state.exp = maxExpCap
        }
        repo.save(state)

        return PresetCompleteResult(
            rewardExp = reward.exp,
            rewardPoints = reward.points,
            alreadyReceived = false,
            streakDays = newStreak
        )
    }

    /**
     * 일정 기간 운동을 하지 않았을 때 경험치 패널티를 적용한다.
     *
     * - 마지막 운동일로부터 3일 이상 경과하면 감소를 시작한다.
     * - 하루당 고정 패널티를 적용하며, 경험치는 0 아래로 내려가지 않는다.
     */
    suspend fun applyInactivityPenaltyIfNeeded() {
        val todayDate = dateProvider()
        val state = repo.load()

        // 기록이 없으면 패널티 없음
        if (state.lastWorkoutDay == 0L) return

        val last = longToLocalDate(state.lastWorkoutDay)

        // 마지막 운동일과 오늘 날짜의 차이 계산
        val days = ChronoUnit.DAYS.between(last, todayDate).toInt()

        // 3일 이상 쉬면 감소 시작
        if (days >= 3) {
            val penaltyPerDay = 20
            val totalPenalty = penaltyPerDay * (days - 2) // 3일부터 적용

            state.exp = maxOf(0, state.exp - totalPenalty)
            repo.save(state)
        }
    }

    /**
     * [LocalDate]를 `yyyyMMdd` 형식의 [Long] 값으로 변환한다.
     *
     * 예: 2025-11-24 → 20251124L
     */
    private fun localDateToLong(date: LocalDate): Long {
        val intValue = date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
        return intValue.toLong()
    }

    /**
     * `yyyyMMdd` 형식의 [Long] 값을 [LocalDate]로 변환한다.
     *
     * 예: 20251124L → LocalDate.of(2025, 11, 24)
     */
    private fun longToLocalDate(value: Long): LocalDate {
        val intValue = value.toInt()
        val year = intValue / 10000
        val month = (intValue % 10000) / 100
        val day = intValue % 100
        return LocalDate.of(year, month, day)
    }

    /**
     * 주어진 레벨에서 필요한 최소 경험치를 반환한다.
     *
     * @param level 조회할 레벨
     * @return 해당 레벨 구간의 시작 경험치
     */
    fun getMinExpForLevel(level: Int): Int {
        val safeLevel = level.coerceIn(1, expTable.size)
        return expTable[safeLevel - 1]
    }

    /**
     * 주어진 레벨에서 다음 레벨로 넘어가기 위한 최대 경험치를 반환한다.
     *
     * 레벨이 마지막이면 마지막 구간의 최대값을 그대로 반환한다.
     *
     * @param level 조회할 레벨
     * @return 다음 레벨로 넘어가는데 필요한 상한 경험치
     */
    fun getMaxExpForLevel(level: Int): Int {
        val safeLevel = level.coerceIn(1, expTable.size)

        return if (safeLevel >= expTable.size) {
            expTable.last()
        } else {
            // 다음 구간의 시작점이 곧 현재 구간의 max
            expTable[safeLevel]
        }
    }

    /**
     * 선택한 프리셋 ID를 저장한다.
     *
     * @param id 선택한 프리셋 ID
     */
    suspend fun selectPreset(id: String) {
        val state = repo.load()
        state.selectedPresetId = id
        repo.save(state)
    }

    /**
     * 새로운 날이 되었을 경우, 오늘 운동 완료 여부를 초기화한다.
     */
    suspend fun resetTodayIfNewDay() {
        val today = localDateToLong(dateProvider())
        val state = repo.load()

        if (state.lastWorkoutDay != today) {
            state.todayComplete = false
            repo.save(state)
        }
    }

    /**
     * 스킨 상점에서 사용할 상태 정보를 구성해 반환한다.
     *
     * - 기본 스킨(0번)은 항상 보유한 것으로 간주하고, 없으면 추가한다.
     *
     * @return 현재 선택 스킨, 보유 스킨 목록, 포인트 정보를 담은 [SkinShopState]
     */
    suspend fun getSkinShopState(): SkinShopState {
        val state = repo.load()
        val owned = ownedSkinRepo.getOwnedIds().toMutableSet()

        // 기본 스킨(0)은 무조건 보유한 걸로 취급
        if (!owned.contains(0)) {
            ownedSkinRepo.addOwned(0)
            owned.add(0)
        }

        return SkinShopState(
            currentSkinId = state.skinId,
            ownedSkinIds = owned,
            points = state.points
        )
    }

    /**
     * 스킨을 구매한다.
     *
     * @param skinId 구매할 스킨 ID
     * @param price  스킨 가격(포인트)
     * @return `true` = 구매 성공, `false` = 구매 실패(포인트 부족/이미 보유)
     */
    suspend fun buySkin(skinId: Int, price: Int): Boolean =
        db.withTransaction {
            val state = repo.load()

            when {
                // 이미 보유 중인 스킨이면 구매 실패 처리
                ownedSkinRepo.isOwned(skinId) -> false

                // 포인트가 부족하면 구매 실패 처리
                state.points < price -> false

                else -> {
                    state.points -= price
                    repo.save(state)
                    ownedSkinRepo.addOwned(skinId)

                    true
                }
            }
        }

    /**
     * 보유 중인 스킨을 선택하여 적용한다.
     *
     * @param skinId 선택할 스킨 ID
     * @return `true` = 선택 성공, `false` = 보유하지 않은 스킨
     */
    suspend fun selectSkin(skinId: Int): Boolean {
        if (!ownedSkinRepo.isOwned(skinId)) return false

        val state = repo.load()
        state.skinId = skinId
        repo.save(state)
        return true
    }

    suspend fun isSkinOwned(skinId: Int): Boolean =
        ownedSkinRepo.isOwned(skinId)
}