package com.github.maharong.growfit

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import androidx.core.content.edit

data class UpdateResult(
    val latestVersion: String,
    val releaseUrl: String
)

class UpdateChecker(context: Context) {

    companion object {
        private const val PREF_NAME = "update_checker"
        private const val KEY_LAST_CHECK_DATE = "last_check_date"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * 오늘 이미 업데이트 체크를 했는지 여부
     */
    fun shouldCheckToday(): Boolean {
        val today = todayString()
        val last = prefs.getString(KEY_LAST_CHECK_DATE, null)
        return last != today
    }

    /**
     * 오늘 체크했다고 기록
     */
    fun markCheckedToday() {
        prefs.edit {
            putString(KEY_LAST_CHECK_DATE, todayString())
        }
    }

    /**
     * GitHub 최신 릴리즈 조회
     */
    suspend fun fetchLatestRelease(
        owner: String,
        repo: String
    ): UpdateResult = withContext(Dispatchers.IO) {

        val api = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val conn = (URL(api).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "GrowFit")
            connectTimeout = 7000
            readTimeout = 7000
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) {
            throw IllegalStateException("GitHub API error ($code): $body")
        }

        val json = JSONObject(body)
        val tagName = json.getString("tag_name") // 예: 1.0.1
        val htmlUrl = json.getString("html_url")

        UpdateResult(
            latestVersion = tagName,
            releaseUrl = htmlUrl
        )
    }

    /**
     * latest > current 인지 비교 (SemVer 기반)
     */
    fun isUpdateAvailable(latest: String, current: String): Boolean {
        fun parse(v: String): List<Int> {
            val parts = v.split(".", "-", "_")
                .take(3)
                .mapNotNull { it.toIntOrNull() }
            return listOf(
                parts.getOrElse(0) { 0 },
                parts.getOrElse(1) { 0 },
                parts.getOrElse(2) { 0 }
            )
        }

        val (lMaj, lMin, lPat) = parse(latest)
        val (cMaj, cMin, cPat) = parse(current)

        return when {
            lMaj != cMaj -> lMaj > cMaj
            lMin != cMin -> lMin > cMin
            else -> lPat > cPat
        }
    }

    private fun todayString(): String =
        LocalDate.now(ZoneId.of("Asia/Seoul")).toString()
}
