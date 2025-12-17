package com.github.maharong.growfit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        val updateChecker = UpdateChecker(applicationContext)

        if (updateChecker.shouldCheckToday()) {
            // 오늘 1회만 체크
            updateChecker.markCheckedToday()

            lifecycleScope.launch {
                runCatching {
                    val result = updateChecker.fetchLatestRelease(
                        owner = "maharong",   // GitHub ID
                        repo = "GrowFit"      // 레포 이름
                    )

                    if (
                        updateChecker.isUpdateAvailable(
                            latest = result.latestVersion,
                            current = BuildConfig.VERSION_NAME
                        )
                    ) {
                        showUpdateDialog(result.latestVersion, result.releaseUrl)
                    }
                }
                // 실패 시 무시
            }
        }
    }

    private fun showUpdateDialog(latestVersion: String, url: String) {
        AlertDialog.Builder(this)
            .setTitle("업데이트가 있어요")
            .setMessage(
                "현재 버전: ${BuildConfig.VERSION_NAME}\n" +
                        "최신 버전: $latestVersion\n\n" +
                        "업데이트 하시겠습니까?"
            )
            .setPositiveButton("업데이트") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .setNegativeButton("나중에", null)
            .show()
    }
}