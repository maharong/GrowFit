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
 * 앱 전역에서 사용할 의존성을 제공하는 Hilt 모듈.
 *
 * - Room Database
 * - UserStateDao
 * - UserStateRepository
 * - UserStateManager
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
            .fallbackToDestructiveMigration(true)
            .build()
    }

    /**
     * UserStateDao 제공
     */
    @Provides
    fun provideUserStateDao(
        db: AppDatabase
    ): UserStateDao = db.userStateDao()

    /**
     * UserStateRepository 제공
     */
    @Provides
    fun provideUserStateRepository(
        dao: UserStateDao
    ): UserStateRepository = UserStateRepository(dao)

    /**
     * UserStateManager 제공
     *
     * - dateProvider는 기본적으로 LocalDate.now()를 사용한다.
     */
    @Provides
    fun provideUserStateManager(
        repo: UserStateRepository
    ): UserStateManager {
        return UserStateManager(
            repo = repo,
            dateProvider = { LocalDate.now() }
        )
    }

    @Provides
    fun providePresetDao(
        db: AppDatabase
    ): PresetDao = db.presetDao()

    @Provides
    fun providePresetRepository(
        dao: PresetDao
    ): PresetRepository = PresetRepository(dao)
}