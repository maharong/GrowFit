package com.github.maharong.growfit

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_state")
data class UserStateEntity(
    @PrimaryKey val id: Int = 0, // 항상 한개만 존재하는 싱글턴 엔티티
    var exp: Int = 0, // 가진 경험치
    var points: Int = 0, // 가진 포인트
    var lastWorkoutDay: Long = 0L, // 마지막 운동 날짜
    var skinId: Int = 0, // 적용된 스킨 id
    var selectedPresetId: String? = null, // 선택된 프리셋 ID
    var todayComplete: Boolean = false, // 오늘 운동 완료 여부
    var streakDays: Int = 0, // 연속 운동일
    // 진동 설정
    var vibrateEnabled: Boolean = true,               // 전체 진동 on/off
    var vibrateLastSecondsEnabled: Boolean = true,    // 마지막 n초 전 진동 on/off
    var vibrateOnStepChange: Boolean = true,          // 다음 스텝 넘어갈 때 진동
    var vibrateOnPresetComplete: Boolean = true,      // 프리셋 완료 시 진동
    var vibrateLastSeconds: Int = 5                   // 마지막 n초의 n 값
)
