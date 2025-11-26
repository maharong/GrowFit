package com.github.maharong.growfit

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 유저 상태(경험치, 포인트, 운동 날짜 등)에 대한
 * 로직을 담당하는 매니저 클래스.
 *
 * - DB 접근은 UserStateRepository가 담당
 */
class UserStateManager(
    private val repo: UserStateRepository,
    private val dateProvider: () -> LocalDate = { LocalDate.now() }
) {
    // 레벨 구간 정의
    private val expTable = listOf(0, 100, 250, 500, 900)

    // 레벨별 리워드 데이터 클래스
    data class LevelReward(val exp: Int, val points: Int)

    // 현재 레벨에 따른 보상량 계산
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

    // 경험치 -> 레벨 계산
    fun getLevel(exp: Int): Int {
        for (i in expTable.indices.reversed()) {
            if (exp >= expTable[i]) return i + 1
        }
        return 1
    }

    // 현재 상태 반환
    suspend fun getCurrentState(): UserStateEntity {
        return repo.load()
    }

    // 프리셋 완료 시 경험치, 포인트 지급
    suspend fun onPresetCompleted() {
        val todayDate = dateProvider()
        val today = localDateToLong(todayDate)
        val state = repo.load()

        // 이미 오늘 보상 받았으면 무시
        if (state.lastWorkoutDay == today) return

        // 현재 레벨 계산
        val level = getLevel(state.exp)

        // 레벨에 따른 리워드 계산
        val reward = getRewardForLevel(level)

        // 오늘 운동 완료 상태로 만들고 마지막 운동 날짜 갱신
        state.todayComplete = true
        state.lastWorkoutDay = today

        // 보상 지급
        state.exp += reward.exp
        state.points += reward.points

        // 경험치가 MAX 레벨 요구량을 넘었을 경우 고정
        val maxExpCap = expTable.last()
        if (state.exp > maxExpCap) {
            state.exp = maxExpCap
        }
        repo.save(state)
    }

    // 일정 기간 운동을 하지 않으면 경험치 감소
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
     * LocalDate → Long(yyyyMMdd 형식) 변환
     * 예: 2025-11-24 → 20251124L
     */
    private fun localDateToLong(date: LocalDate): Long {
        val intValue = date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
        return intValue.toLong()
    }

    /**
     * Long(yyyyMMdd 형식) → LocalDate 변환
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
     * 현재 레벨에서 필요한 최소 경험치 (구간 시작점)
     */
    fun getMinExpForLevel(level: Int): Int {
        val safeLevel = level.coerceIn(1, expTable.size)
        return expTable[safeLevel - 1]
    }

    /**
     * 현재 레벨에서 다음 레벨로 넘어가기 위한 최대 경험치
     * (레벨이 마지막이면 마지막 구간의 최대값을 그대로 반환)
     */
    fun getMaxExpForLevel(level: Int): Int {
        val safeLevel = level.coerceIn(1, expTable.size)

        return if (safeLevel >= expTable.size) {
            expTable.last()
        } else {
            expTable[safeLevel]   // 다음 구간의 시작점이 곧 현재 구간의 max
        }
    }

    // 프리셋 선택 함수
    suspend fun selectPreset(id: String) {
        val state = repo.load()
        state.selectedPresetId = id
        state.todayComplete = false
        repo.save(state)
    }

    // 오늘 운동 여부 초기화 함수
    suspend fun resetTodayIfNewDay() {
        val today = localDateToLong(dateProvider())
        val state = repo.load()

        if (state.lastWorkoutDay != today) {
            state.todayComplete = false
            repo.save(state)
        }
    }
}