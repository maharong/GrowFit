package com.github.maharong.growfit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt 의존성 그래프를 초기화하는 Application 클래스.
 * - @HiltAndroidApp 사용해야 Hilt가 전체 앱에서 동작함
 */
@HiltAndroidApp
class GrowFitApp : Application()