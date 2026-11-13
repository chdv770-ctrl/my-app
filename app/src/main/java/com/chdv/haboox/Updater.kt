package com.chdv.haboox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-update from GitHub Releases. Every successful build publishes a release tagged v2.0.<run>
 * with HaBoox.apk attached; all builds are signed with the same key, so an update installs over
 * the existing app and keeps its settings.
 */
object Updater {
    private const val REPO = "chdv770-ctrl/my-app"
    private const val API = "https://api.github.com/repos/$REPO/releases/latest"

    @Volatile
    private var busy = false

    private fun build(v: String): Int = v.trim().removePrefix("v").substringAfterLast('.').toIntOrNull() ?: 0

    private fun localVersion(a: Activity): String = try {
        a.packageManager.getPackageInfo(a.packageName, 0).versionName ?: "0"
    } catch (e: Exception) { "0" }

    fun dailyCheck(a: Activity) {
        val last = try { Prefs.s("lastUpdateCheck", "0").toLong() } catch (e: Exception) { 0L }
        if (System.currentTimeMillis() - last < 24L * 3600 * 1000) return
        Prefs.put("lastUpdateCheck", System.currentTimeMillis().toString())
        check(a, silent = true)
    }

    private fun open(u: String): HttpURLConnection {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.instanceFollowRedirects = false
        c.setRequestProperty("User-Agent", "HaBoox-Updater")
        c.setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream, */*")
        return c
    }

    /** GET that follows redirects across hosts (GitHub sends downloads to another domain). */
    private fun get(url: String): HttpURLConnection {
        var u = url
        repeat(6) {
            val c = open(u)
            val code = c.responseCode
            if (code in 300..399) {
                val loc = c.getHeaderField("Location")
                c.disconnect()
                if (loc != null) u = URL(URL(u), loc).toString()
                return@repeat
            }
            return c
        }
        return open(u)
    }
    fun check(a: Activity, silent: Boolean) {
        if (busy) return
        busy = true
        if (!silent) a.toast("בודק אם יש עדכון…")
        Thread {
            try {
                val c = get(API)
                val body = c.inputStream.bufferedReader().use { it.readText() }
                c.disconnect()
                val tag = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: ""
                val apk = Regex("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"").find(body)?.groupValues?.get(1)
                val local = localVersion(a)
                val newer = build(tag) > build(local)
                a.runOnUiThread {
                    busy = false
                    if (newer && apk != null) {
                        Sheet.confirm(a, "יש עדכון זמין",
                            "גרסה חדשה: ${tag.removePrefix("v")}\nהגרסה שלך: $local\n\nההורדה דרך האינטרנט (Wi‑Fi), וההגדרות שלך יישמרו.",
                            "הורדה והתקנה") { download(a, apk) }
                    } else if (!silent) {
                        Sheet.info(a, "אין עדכון חדש", "הגרסה שלך ($local) היא העדכנית ביותר.")
                    }
                }
            } catch (e: Exception) {
                a.runOnUiThread {
                    busy = false
                    if (!silent) Sheet.info(a, "לא ניתן לבדוק עדכונים", "ודאו שהמכשיר מחובר לאינטרנט (Wi‑Fi) ונסו שוב.")
                }
            }
        }.start()
    }

    private fun download(a: Activity, url: String) {
        if (Build.VERSION.SDK_INT >= 26 && !a.packageManager.canRequestPackageInstalls()) {
            Sheet.confirm(a, "צריך אישור חד-פעמי",
                "כדי להתקין עדכונים יש לאשר להיכל הספרים להתקין אפליקציות. אחרי האישור חזרו ולחצו שוב על \"בדיקת עדכונים\".",
                "פתיחת ההגדרה") {
                a.go(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.packageName)))
            }
            return
        }
        a.toast("מוריד את העדכון…")
        Thread {
            try {
                val dir = File(a.cacheDir, "update").apply { mkdirs() }
                val out = File(dir, "HaBoox.apk")
                val c = get(url)
                val total = c.contentLength.toLong()
                var done = 0L
                var lastPct = -1
                c.inputStream.use { input ->
                    FileOutputStream(out).use { o ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n)
                            done += n
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct / 25 != lastPct / 25) {
                                    lastPct = pct
                                    a.runOnUiThread { a.toast("מוריד… $pct%") }
                                }
                            }
                        }
                    }
                }
                c.disconnect()
                a.runOnUiThread { install(a, out) }
            } catch (e: Exception) {
                a.runOnUiThread { Sheet.info(a, "ההורדה נכשלה", "בדקו את החיבור לאינטרנט ונסו שוב.") }
            }
        }.start()
    }

    private fun install(a: Activity, f: File) {
        val uri = FileProvider.getUriForFile(a, a.packageName + ".files", f)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!safeOpenTry(a, i)) a.toast("לא ניתן לפתוח את מתקין האפליקציות")
    }
}
