package com.github.maharong.growfit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * 네비게이션 호스트 역할을 하는 메인 Activity.
 * - Hilt 주입
 * - activity_main.xml 내 NavHostFragment 디스플레이
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}