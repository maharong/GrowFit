package com.github.maharong.growfit

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room 메인 데이터베이스 클래스.
 *
 * - entities : 이 DB가 관리하는 테이블(엔티티) 목록
 * - version  : 스키마 버전 (테이블 구조 바꾸면 올려야 함)
 */
@Database(
    entities = [UserStateEntity::class, PresetEntity::class, PresetStepEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * 유저 상태 테이블에 접근하기 위한 DAO.
     */
    abstract fun userStateDao(): UserStateDao
    /**
     * 프리셋 테이블에 접근하기 위한 DAO.
     */
    abstract fun presetDao(): PresetDao
}