package com.github.maharong.growfit

/**
 * 운동 스텝의 종류를 정의한다.
 *
 * - TIME   : 시간 기반 동작
 * - COUNT  : 횟수 기반 동작
 * - REST   : 휴식
 * - WALKING: 걷기
 * - RUNNING: 달리기
 */
enum class StepType {
    TIME, // 시간 기반
    COUNT, // 횟수
    REST, // 휴식
    WALKING, // 걷기
    RUNNING // 달리기
}