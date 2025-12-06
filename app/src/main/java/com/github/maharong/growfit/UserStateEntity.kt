package com.github.maharong.growfit

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 앱 전체에서 하나만 존재하는 유저 상태 엔티티.
 *
 * - 경험치, 포인트
 * - 오늘 운동 완료 여부
 * - 프리셋 선택 정보
 * - 진동 설정
 * 등을 저장한다.
 */
@Entity(tableName = "user_state")
data class UserStateEntity(

    /** 항상 한 개만 존재하는 싱글턴 엔티티의 고정 ID (0) */
    @PrimaryKey val id: Int = 0,

    /** 누적 경험치 */
    var exp: Int = 0,

    /** 보유 포인트 */
    var points: Int = 0,

    /** 마지막 운동 날짜(yyyyMMdd 형식 Long) */
    var lastWorkoutDay: Long = 0L,

    /** 현재 적용된 스킨 ID */
    var skinId: Int = 0,

    /** 선택된 프리셋 ID (없으면 null) */
    var selectedPresetId: String? = null,

    /** 오늘 운동을 완료했는지 여부 */
    var todayComplete: Boolean = false,

    /** 연속 운동일(스트릭 일수) */
    var streakDays: Int = 0,

    // --- 진동 설정 ---

    /** 전체 진동 on/off */
    var vibrateEnabled: Boolean = true,

    /** 마지막 n초 전 진동 on/off */
    var vibrateLastSecondsEnabled: Boolean = true,

    /** 다음 스텝으로 넘어갈 때 진동 on/off */
    var vibrateOnStepChange: Boolean = true,

    /** 프리셋 전체 완료 시 진동 on/off */
    var vibrateOnPresetComplete: Boolean = true,

    /** 마지막 n초의 n 값 (예: 5초 전) */
    var vibrateLastSeconds: Int = 5
)
