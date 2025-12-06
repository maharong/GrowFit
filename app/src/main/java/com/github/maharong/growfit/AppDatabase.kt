package com.github.maharong.growfit

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room 메인 데이터베이스.
 *
 * - entities: DB가 관리할 엔티티 목록
 * - version : 스키마 버전
 * - exportSchema: 스키마 파일을 저장하지 않도록 false 설정
 */
@Database(
    entities = [
        UserStateEntity::class,
        PresetEntity::class,
        PresetStepEntity::class,
        OwnedSkinEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * 유저 상태 테이블에 접근하기 위한 DAO
     */
    abstract fun userStateDao(): UserStateDao
    /**
     * 프리셋 테이블에 접근하기 위한 DAO
     */
    abstract fun presetDao(): PresetDao
    /**
     * 구매한 스킨 테이블에 접근하기 위한 DAO
     */
    abstract fun ownedSkinDao(): OwnedSkinDao
}