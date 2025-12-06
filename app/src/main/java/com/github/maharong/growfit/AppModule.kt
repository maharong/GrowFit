package com.github.maharong.growfit

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Singleton
import kotlin.jvm.java

/**
 * 앱 전역에서 필요한 DI 의존성을 제공하는 Hilt 모듈.
 *
 * - Room Database
 * - 각 DAO
 * - Repository
 * - UserStateManager (dateProvider 포함)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Room Database 싱글톤 제공
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "growfit-db"
        )
            // TODO: 다음 스키마 변경 시 migration 추가
            .build()
    }

    /** 유저 상태 DAO/Repo/Manager */
    @Provides
    fun provideUserStateDao(
        db: AppDatabase
    ): UserStateDao = db.userStateDao()
    @Provides
    fun provideUserStateRepository(
        dao: UserStateDao
    ): UserStateRepository = UserStateRepository(dao)

    /**
     * UserStateManager 제공
     * - dateProvider를 분리하여 테스트 가능하게 구성
     */
    @Provides
    fun provideUserStateManager(
        repo: UserStateRepository,
        ownedSkinRepository: OwnedSkinRepository
    ): UserStateManager {
        return UserStateManager(
            repo = repo,
            ownedSkinRepo = ownedSkinRepository,
            dateProvider = { LocalDate.now() }
        )
    }

    /** 프리셋 DAO/Repo */
    @Provides
    fun providePresetDao(
        db: AppDatabase
    ): PresetDao = db.presetDao()
    @Provides
    fun providePresetRepository(
        dao: PresetDao
    ): PresetRepository = PresetRepository(dao)

    /** 스킨 DAO/Repo */
    @Provides
    fun provideOwnedSkinDao(
        db: AppDatabase
    ): OwnedSkinDao = db.ownedSkinDao()
    @Provides
    fun provideOwnedSkinRepository(
        dao: OwnedSkinDao
    ): OwnedSkinRepository = OwnedSkinRepository(dao)
}