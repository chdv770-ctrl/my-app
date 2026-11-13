package com.chdv.booxhome

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Minimal home screen: "ב"ה" header and a grid of installed apps.
 * Black-and-white, portrait, e-ink friendly (no animations).
 * Books, files, folders, widgets and the settings screen come in later steps.
 */
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(32, 32, 32, 32)
        }

        root.addView(TextView(this).apply {
            text = "ב\"ה"
            textSize = 16f
            setTextColor(Color.BLACK)
            gravity = Gravity.END
        })

        val grid = GridLayout(this).apply {
            columnCount = 4
        }
        val scroll = android.widget.ScrollView(this).apply {
            addView(grid)
        }
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(root)
        loadApps(grid)
    }

    private fun loadApps(grid: GridLayout) {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo }
            .filter { it.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString() }

        for (info in apps) {
            val label = info.loadLabel(pm).toString()
            val tile = TextView(this).apply {
                text = label
                textSize = 13f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                setOnClickListener {
                    pm.getLaunchIntentForPackage(info.packageName)?.let { startActivity(it) }
                }
            }
            grid.addView(tile)
        }
    }
}
